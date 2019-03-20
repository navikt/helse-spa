package no.nav.helse

import assertk.assert
import assertk.assertions.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.behandling.*
import no.nav.helse.domain.Arbeidsforhold
import no.nav.helse.domain.ArbeidsforholdWrapper
import no.nav.helse.domain.Arbeidsgiver
import no.nav.helse.fastsetting.*
import no.nav.helse.oppslag.*
import no.nav.helse.streams.JsonSerializer
import no.nav.helse.streams.Topics.SYKEPENGEBEHANDLINGSFEIL
import no.nav.helse.streams.Topics.SYKEPENGESØKNADER_INN
import no.nav.helse.streams.Topics.VEDTAK_SYKEPENGER
import no.nav.helse.streams.defaultObjectMapper
import no.nav.helse.sykepenger.beregning.Beregningsresultat
import no.nav.nare.core.evaluations.Evaluering
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDate.parse
import java.time.LocalDateTime
import kotlin.collections.HashMap

class EndToEndTest {

    companion object {
        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        val embeddedEnvironment = KafkaEnvironment(
                users = listOf(JAASCredential(username, password)),
                autoStart = false,
                withSchemaRegistry = false,
                withSecurity = true,
                topics = listOf(
                        SYKEPENGESØKNADER_INN_LEGACY.name,
                        SYKEPENGESØKNADER_INN.name,
                        VEDTAK_SYKEPENGER.name,
                        SYKEPENGEBEHANDLINGSFEIL.name
                )
        )

        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        private lateinit var app: SaksbehandlingStream

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
            embeddedEnvironment.start()

            startSpa()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            stopSpa()

            server.stop()
            embeddedEnvironment.tearDown()
        }

        private fun startSpa() {
            val env = Environment(
                    username = username,
                    password = password,
                    kafkaUsername = username,
                    kafkaPassword = password,
                    bootstrapServersUrl = embeddedEnvironment.brokersURL,
                    sparkelBaseUrl = server.baseUrl(),
                    stsRestUrl = server.baseUrl()
            )

            app = SaksbehandlingStream(env)
            app.start()
        }

        private fun stopSpa() {
            app.stop()
        }
    }

    @BeforeEach
    fun configure() {
        configureFor(server.port())
    }

    @Test
    fun `behandle en søknad`() {
        val aktørId = "11987654321"

        println("Kafka: ${embeddedEnvironment.brokersURL}")
        println("Zookeeper: ${embeddedEnvironment.serverPark.zookeeper.host}:${embeddedEnvironment.serverPark.zookeeper.port}")

        restStsStub()
        personStub(aktørId)
        inntektStub(aktørId)
        arbeidsforholdStub(aktørId)

        val innsendtSøknad = produserSykepengesøknadV2(aktørId)

        val vedtak: SykepengeVedtak = ventPåVedtak()!!

        checkSøknad(innsendtSøknad, vedtak.originalSøknad)
        checkAvklarteVerdier(innsendtSøknad, vedtak.faktagrunnlag, vedtak.avklarteVerdier)
// TODO       checkBeregning(sykepengeberegning.beregning)
// TODO       checkFaktagrunnlag(sykepengeberegning.faktagrunnlag)
// TODO       checkVilkårsprøving(sykepengeberegning.vilkårsprøving)
    }

    private fun checkSøknad(innsendtSøknad: SykepengesøknadV2DTO, faktiskSøknad: Sykepengesøknad) {
        assert(innsendtSøknad.aktorId).isEqualTo(faktiskSøknad.aktorId)
        assert(innsendtSøknad.status).isEqualTo(faktiskSøknad.status)
        assert(innsendtSøknad.arbeidsgiver).isEqualTo(faktiskSøknad.arbeidsgiver)
        assert(innsendtSøknad.soktUtenlandsopphold).isEqualTo(faktiskSøknad.soktUtenlandsopphold)
        assert(innsendtSøknad.fom).isEqualTo(faktiskSøknad.fom)
        assert(innsendtSøknad.tom).isEqualTo(faktiskSøknad.tom)
        assert(innsendtSøknad.startSyketilfelle).isEqualTo(faktiskSøknad.startSyketilfelle)
        assert(innsendtSøknad.sendtNav).isEqualTo(faktiskSøknad.sendtNav)
        assert(innsendtSøknad.soknadsperioder).isEqualTo(faktiskSøknad.soknadsperioder)
    }

    private fun checkAvklarteVerdier(innsendtSøknad: SykepengesøknadV2DTO, faktagrunnlag: Faktagrunnlag, avklarteVerdier: AvklarteVerdier) {
        checkAlder(avklarteVerdier.alder)
        checkMaksdato(innsendtSøknad, avklarteVerdier.alder.fastsattVerdi, faktagrunnlag.sykepengeliste, avklarteVerdier.maksdato)
        checkMedlemsskap(avklarteVerdier.medlemsskap)
        checkSykepengegrunnlag(avklarteVerdier.sykepengegrunnlag, innsendtSøknad.startSyketilfelle)
        checkArbeidsforhold(avklarteVerdier.arbeidsforhold)
        // TODO checkOpptjeningstid(avklarteVerdier.opptjeningstid)
        // TODO checkSykepengeliste(avklarteVerdier.sykepengeliste)
        // TODO checkOpptjeningstid(avklarteVerdier.opptjeningstid)
    }

    private fun checkAlder(aldersVurdering: Vurdering.Avklart<Alder, Aldersgrunnlag>) {
        assert(aldersVurdering.fastsattVerdi).isEqualTo(48)
        assert(aldersVurdering.begrunnelse).isEqualTo(begrunnelse_p_8_51)
        assert(aldersVurdering.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(aldersVurdering.fastsattAv).isEqualTo("SPA")

        assert(aldersVurdering.grunnlag.fodselsdato).isEqualTo(stubbet_person.fdato)
    }

    private fun checkMaksdato(innsendtSøknad: SykepengesøknadV2DTO, alder: Alder, sykepengeListe: Collection<SykepengerPeriode>, maksdato: Vurdering.Avklart<LocalDate, Grunnlagsdata>) {
        val forventetMaksdato = LocalDate.of(2019, 12, 12)

        assert(maksdato.fastsattVerdi).isEqualTo(forventetMaksdato)
        assert(maksdato.begrunnelse).isEqualTo("§ 8-12: ARBEIDSTAKER på 48 år gir maks 248 dager. 0 av disse er forbrukt")
        assert(maksdato.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(maksdato.fastsattAv).isEqualTo("SPA")

        val grunnlag = maksdato.grunnlag
        assert(grunnlag.førsteFraværsdag).isEqualTo(innsendtSøknad.startSyketilfelle)
        assert(grunnlag.førsteSykepengedag).isEqualTo(innsendtSøknad.fom)
        assert(grunnlag.personensAlder).isEqualTo(alder)
        assert(grunnlag.yrkesstatus).isEqualTo(Yrkesstatus.ARBEIDSTAKER)
        assert(grunnlag.tidligerePerioder).isEqualTo(sykepengeListe)
    }

    private fun checkMedlemsskap(medlemsskap: Vurdering.Avklart<Boolean, Medlemsskapgrunnlag>) {
        assert(medlemsskap.fastsattVerdi).isTrue()
        assert(medlemsskap.begrunnelse).contains(søkerErBosattINorge)
        assert(medlemsskap.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(medlemsskap.fastsattAv).isEqualTo("SPA")

        assert(medlemsskap.grunnlag.bostedsland).isEqualTo(landskodeNORGE)
    }

    private fun checkSykepengegrunnlag(sykepengegrunnlagVurdering: Vurdering.Avklart<Sykepengegrunnlag, Beregningsperiode>, startSyketilfelle: LocalDate) {
        checkSykepengegrunnlagNårTrygdenYter(sykepengegrunnlagVurdering.fastsattVerdi.sykepengegrunnlagNårTrygdenYter, startSyketilfelle)
        checkSykepengegrunnlagIArbeidsgiverperioden(sykepengegrunnlagVurdering.fastsattVerdi.sykepengegrunnlagIArbeidsgiverperioden, startSyketilfelle)
        assert(sykepengegrunnlagVurdering.begrunnelse).isEqualTo("")
        assert(sykepengegrunnlagVurdering.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(sykepengegrunnlagVurdering.fastsattAv).isEqualTo("SPA")

        checkBeregningsperiode12mnd(sykepengegrunnlagVurdering.grunnlag)
    }

    private fun checkSykepengegrunnlagNårTrygdenYter(sykepengegrunnlagNårTrygdenYterVurdering: Vurdering.Avklart<Long, Beregningsperiode>, førsteSykdomsdag: LocalDate) {
        assert(sykepengegrunnlagNårTrygdenYterVurdering.fastsattVerdi).isEqualTo(300000L)
        assert(sykepengegrunnlagNårTrygdenYterVurdering.begrunnelse).contains(paragraf_8_30_første_ledd)
        assert(sykepengegrunnlagNårTrygdenYterVurdering.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(sykepengegrunnlagNårTrygdenYterVurdering.fastsattAv).isEqualTo("SPA")

        checkBeregningsperiode3mnd(sykepengegrunnlagNårTrygdenYterVurdering.grunnlag, førsteSykdomsdag)
    }

    private fun checkSykepengegrunnlagIArbeidsgiverperioden(sykepengegrunnlagIArbeidsgiverperiodenVurdering: Vurdering.Avklart<Long, Beregningsperiode>, førsteSykdomsdag: LocalDate) {
        assert(sykepengegrunnlagIArbeidsgiverperiodenVurdering.fastsattVerdi).isEqualTo(25000L)
        assert(sykepengegrunnlagIArbeidsgiverperiodenVurdering.begrunnelse).contains(paragraf_8_28_andre_ledd)
        assert(sykepengegrunnlagIArbeidsgiverperiodenVurdering.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(sykepengegrunnlagIArbeidsgiverperiodenVurdering.fastsattAv).isEqualTo("SPA")

        checkBeregningsperiode3mnd(sykepengegrunnlagIArbeidsgiverperiodenVurdering.grunnlag, førsteSykdomsdag)
    }

    private fun checkBeregningsperiode3mnd(beregningsperiode: Beregningsperiode, førsteSykdomsdag: LocalDate) {
        assert(beregningsperiode.begrunnelse).isEqualTo(paragraf_8_28_tredje_ledd_bokstav_a + "(${førsteSykdomsdag}) legges til grunn.")
        assert(beregningsperiode.inntekter).hasSize(3)
        assert(beregningsperiode.inntekter).each {
            val inntekt = it.actual
            assert(inntekt.arbeidsgiver.orgnr).isEqualTo("97114455")
            assert(inntekt.beløp.toLong()).isEqualTo(25000L)
            assert(inntekt.opptjeningsperiode.fom).isBetween(LocalDate.of(2018, 10, 1), LocalDate.of(2018, 12, 1))
            assert(inntekt.opptjeningsperiode.tom).isBetween(LocalDate.of(2018, 10, 30), LocalDate.of(2018, 12, 31))
        }
    }

    private fun checkBeregningsperiode12mnd(beregningsperiode: Beregningsperiode) {
        assert(beregningsperiode.begrunnelse).isNotEmpty()
        assert(beregningsperiode.inntekter).hasSize(12)
        assert(beregningsperiode.inntekter).each {
            val inntekt = it.actual
            assert(inntekt.arbeidsgiver.orgnr).isEqualTo("97114455")
            assert(inntekt.beløp.toLong()).isEqualTo(25000L)
            assert(inntekt.opptjeningsperiode.fom).isBetween(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 1))
            assert(inntekt.opptjeningsperiode.tom).isBetween(LocalDate.of(2018, 1, 31), LocalDate.of(2018, 12, 31))
        }
    }

    private fun checkArbeidsforhold(arbeidsforholdVurdering: Vurdering.Avklart<Boolean, List<Arbeidsforhold>>) {
        assert(arbeidsforholdVurdering.fastsattVerdi).isTrue()
        assert(arbeidsforholdVurdering.begrunnelse).isEqualTo(søker_har_arbeidsgiver + stubbet_arbeidsforhold.arbeidsgiver.orgnummer)
        assert(arbeidsforholdVurdering.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(arbeidsforholdVurdering.fastsattAv).isEqualTo("SPA")

        val arbeidsforhold = arbeidsforholdVurdering.grunnlag
        assert(arbeidsforhold.size).isEqualTo(1)
        assert(arbeidsforhold.get(0)).isEqualTo(stubbet_arbeidsforhold)
    }

    /**   "opptjeningstid": {
    "grunnlag": {
    "førsteSykdomsdag": "2019-01-01",
    "arbeidsforhold": [
    {
    "navn": "EQUINOR ASA, AVD STATOIL SOKKELVIRKSOMHET",
    "organisasjonsnummer": "97114455",
    "startdato":"2017-01-01"
    }
    ]
    },
    "begrunnelse": "Søker er i et aktivt arbeidsforhold",
    "fastsattVerdi": 730,
    "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
    "fastsattAv": "SPA"
    },*/
    private fun checkOpptjeningstid(opptjeningstid: Vurdering.Avklart<Opptjeningstid, Opptjeningsgrunnlag>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**  "sykepengeliste": [],
     */
    private fun checkSykepengeliste(sykepengeliste: Collection<SykepengerPeriode>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun checkBeregning(beregning: Beregningsresultat) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun checkFaktagrunnlag(faktagrunnlag: Faktagrunnlag) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun checkVilkårsprøving(vilkårsprøving: Evaluering) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun forventetVedtak() = """
{
  "maksdato": {
    "grunnlag": {
      "førsteFraværsdag": "2019-01-01",
      "førsteSykepengedag": "2019-01-01",
      "personensAlder": 48,
      "yrkesstatus": "ARBEIDSTAKER",
      "tidligerePerioder": []
    },
    "begrunnelse": "§ 8-12: ARBEIDSTAKER på 48 år gir maks 248 dager. 0 av disse er forbrukt",
    "fastsattVerdi": "2019-12-12",
    "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
    "fastsattAv": "SPA"
  },
  "medlemsskap": {
    "grunnlag": {
      "bostedsland": "NOR"
    },
    "begrunnelse": "(Søker er bosatt i Norge. ELLER Vi har ikke nok informasjon til å kunne gi et entydig svar.)",
    "fastsattVerdi": true,
    "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
    "fastsattAv": "SPA"
  },
  "sykepengegrunnlag": {
    "begrunnelse": "",
    "grunnlag": {
      "begrunnelse": "§ 8-30 andre ledd – rapportert inntekt (se § 8-29) til a-ordningen etter reglene i a-opplysningsloven de siste tolv kalendermånedene før arbeidsuførheten inntraff (2019-01-01) legges til grunn.",
      "inntekter": [
        {
          "arbeidsgiver": {
            "orgnr": "97114455"
          },
          "beløp": 25000,
          "opptjeningsperiode": {
            "tom": "2018-12-31",
            "fom": "2018-12-01"
          }
        },
        {
          "arbeidsgiver": {
            "orgnr": "97114455"
          },
          "beløp": 25000,
          "opptjeningsperiode": {
            "tom": "2018-11-30",
            "fom": "2018-11-01"
          }
        },
        {
          "arbeidsgiver": {
            "orgnr": "97114455"
          },
          "beløp": 25000,
          "opptjeningsperiode": {
            "tom": "2018-10-31",
            "fom": "2018-10-01"
          }
        },
        {
          "arbeidsgiver": {
            "orgnr": "97114455"
          },
          "beløp": 25000,
          "opptjeningsperiode": {
            "tom": "2018-09-30",
            "fom": "2018-09-01"
          }
        },
        {
          "arbeidsgiver": {
            "orgnr": "97114455"
          },
          "beløp": 25000,
          "opptjeningsperiode": {
            "tom": "2018-08-31",
            "fom": "2018-08-01"
          }
        },
        {
          "arbeidsgiver": {
            "orgnr": "97114455"
          },
          "beløp": 25000,
          "opptjeningsperiode": {
            "tom": "2018-07-31",
            "fom": "2018-07-01"
          }
        },
        {
          "arbeidsgiver": {
            "orgnr": "97114455"
          },
          "beløp": 25000,
          "opptjeningsperiode": {
            "tom": "2018-06-30",
            "fom": "2018-06-01"
          }
        },
        {
          "arbeidsgiver": {
            "orgnr": "97114455"
          },
          "beløp": 25000,
          "opptjeningsperiode": {
            "tom": "2018-05-31",
            "fom": "2018-05-01"
          }
        },
        {
          "arbeidsgiver": {
            "orgnr": "97114455"
          },
          "beløp": 25000,
          "opptjeningsperiode": {
            "tom": "2018-04-30",
            "fom": "2018-04-01"
          }
        },
        {
          "arbeidsgiver": {
            "orgnr": "97114455"
          },
          "beløp": 25000,
          "opptjeningsperiode": {
            "tom": "2018-03-31",
            "fom": "2018-03-01"
          }
        },
        {
          "arbeidsgiver": {
            "orgnr": "97114455"
          },
          "beløp": 25000,
          "opptjeningsperiode": {
            "tom": "2018-02-28",
            "fom": "2018-02-01"
          }
        },
        {
          "arbeidsgiver": {
            "orgnr": "97114455"
          },
          "beløp": 25000,
          "opptjeningsperiode": {
            "tom": "2018-01-31",
            "fom": "2018-01-01"
          }
        }
      ]
    },
    "fastsattVerdi": {
      "sykepengegrunnlagNårTrygdenYter": {
        "begrunnelse": "§ 8-30 første ledd",
        "grunnlag": {
          "begrunnelse": "§ 8-28 tredje ledd bokstav a) \u2013 De tre siste kalendermånedene før arbeidstakeren ble arbeidsufør (2019-01-01) legges til grunn.",
          "inntekter": [
            {
              "arbeidsgiver": {
                "orgnr": "97114455"
              },
              "beløp": 25000,
              "opptjeningsperiode": {
                "tom": "2018-12-31",
                "fom": "2018-12-01"
              }
            },
            {
              "arbeidsgiver": {
                "orgnr": "97114455"
              },
              "beløp": 25000,
              "opptjeningsperiode": {
                "tom": "2018-11-30",
                "fom": "2018-11-01"
              }
            },
            {
              "arbeidsgiver": {
                "orgnr": "97114455"
              },
              "beløp": 25000,
              "opptjeningsperiode": {
                "tom": "2018-10-31",
                "fom": "2018-10-01"
              }
            }
          ]
        },
        "fastsattVerdi": 300000,
        "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
        "fastsattAv": "spa"
      },
      "sykepengegrunnlagIArbeidsgiverperioden": {
        "begrunnelse": "§ 8-28 andre ledd",
        "grunnlag": {
          "begrunnelse": "§ 8-28 tredje ledd bokstav a) \u2013 De tre siste kalendermånedene før arbeidstakeren ble arbeidsufør (2019-01-01) legges til grunn.",
          "inntekter": [
            {
              "arbeidsgiver": {
                "orgnr": "97114455"
              },
              "beløp": 25000,
              "opptjeningsperiode": {
                "tom": "2018-12-31",
                "fom": "2018-12-01"
              }
            },
            {
              "arbeidsgiver": {
                "orgnr": "97114455"
              },
              "beløp": 25000,
              "opptjeningsperiode": {
                "tom": "2018-11-30",
                "fom": "2018-11-01"
              }
            },
            {
              "arbeidsgiver": {
                "orgnr": "97114455"
              },
              "beløp": 25000,
              "opptjeningsperiode": {
                "tom": "2018-10-31",
                "fom": "2018-10-01"
              }
            }
          ]
        },
        "fastsattVerdi": 25000,
        "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
        "fastsattAv": "spa"
      }
    },
    "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
    "fastsattAv": "SPA"
  },
  "arbeidsforhold": {
    "grunnlag": {
      "arbeidsgivere": [
        {
          "navn": "EQUINOR ASA, AVD STATOIL SOKKELVIRKSOMHET",
          "organisasjonsnummer": "97114455",
          "startdato":"2017-01-01"
        }
      ]
    },
    "begrunnelse": "Søker har en arbeidsgiver med orgnummer 97114455",
    "fastsattVerdi": true,
    "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
    "fastsattAv": "SPA"
  },
  "opptjeningstid": {
    "grunnlag": {
      "førsteSykdomsdag": "2019-01-01",
      "arbeidsforhold": [
        {
          "navn": "EQUINOR ASA, AVD STATOIL SOKKELVIRKSOMHET",
          "organisasjonsnummer": "97114455",
          "startdato":"2017-01-01"
        }
      ]
    },
    "begrunnelse": "Søker er i et aktivt arbeidsforhold",
    "fastsattVerdi": 730,
    "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
    "fastsattAv": "SPA"
  },
  "sykepengeliste": [],
    "aktorId": "11987654321",
    "tom": "2019-01-31",
    "arbeidsgiver": {
      "orgnummer": "97114455",
      "navn": "NAV"
    },
    "soktUtenlandsopphold": false,
    "fom": "2019-01-01",
    "soknadsperioder": [
      {
        "tom": "2019-01-31",
        "fom": "2019-01-01",
        "sykmeldingsgrad": 100
      }
    ],
    "harVurdertInntekt": false,
    "startSyketilfelle": "2019-01-01",
    "sendtNav": "2019-01-17T00:00",

  "alder": {
    "grunnlag": {
      "fodselsdato": "1970-09-01"
    },
    "begrunnelse": "§ 8-51",
    "fastsattVerdi": 48,
    "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
    "fastsattAv": "SPA"
  }
}
""".trimIndent()

    private fun produserSykepengesøknadV2(aktørId: String): SykepengesøknadV2DTO {
        val søknad = SykepengesøknadV2DTO(
                aktorId = aktørId,
                status = "SENDT",
                arbeidsgiver = Arbeidsgiver(
                        navn = "EQUINOR ASA, AVD STATOIL SOKKELVIRKSOMHET",
                        orgnummer = "97114455"
                ),
                fom = parse("2019-01-01"),
                tom = parse("2019-01-31"),
                startSyketilfelle = parse("2019-01-01"),
                sendtNav = parse("2019-01-17").atStartOfDay(),
                soknadsperioder = listOf(
                        Soknadsperiode(
                                fom = parse("2019-01-01"),
                                tom = parse("2019-01-31"),
                                sykmeldingsgrad = 100
                        )
                ),
                soktUtenlandsopphold = false
        )
        produceOneMessage(søknad)
        return søknad
    }

    class SykepengeVedtakDeserializer : Deserializer<SykepengeVedtak> {
        private val log = LoggerFactory.getLogger("SykepengeVedtakDeserializer")

        override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}

        override fun deserialize(topic: String?, data: ByteArray?): SykepengeVedtak? {
            return data?.let {
                try {
                    defaultObjectMapper.readValue<SykepengeVedtak>(it)
                } catch (e: Exception) {
                    log.warn("Not a valid json", e)
                    null
                }
            }
        }

        override fun close() {}

    }

    private fun ventPåVedtak(): SykepengeVedtak? {
        val resultConsumer = KafkaConsumer<String, SykepengeVedtak>(consumerProperties(), StringDeserializer(), SykepengeVedtakDeserializer())
        resultConsumer.subscribe(listOf(VEDTAK_SYKEPENGER.name))

        val end = System.currentTimeMillis() + 60 * 1000

        while (System.currentTimeMillis() < end) {
            resultConsumer.seekToBeginning(resultConsumer.assignment())
            val records = resultConsumer.poll(Duration.ofSeconds(1))

            if (!records.isEmpty) {
                assertEquals(1, records.count())

                return records.records(VEDTAK_SYKEPENGER.name).map {
                    it.value()
                }.first()
            }
        }

        return null
    }

    private fun consumerProperties(): MutableMap<String, Any>? {
        return HashMap<String, Any>().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";")
            put(ConsumerConfig.GROUP_ID_CONFIG, "spa-e2e-verification")
        }
    }

    private fun produceOneMessage(message: SykepengesøknadV2DTO) {
        val producer = KafkaProducer<String, JsonNode>(producerProperties(), StringSerializer(), JsonSerializer())
        producer.send(ProducerRecord(SYKEPENGESØKNADER_INN.name, null, defaultObjectMapper.valueToTree(message)))
        producer.flush()
    }

    private fun producerProperties(): MutableMap<String, Any>? {
        return HashMap<String, Any>().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";")
        }
    }

    private fun restStsStub() {
        stubFor(any(urlPathEqualTo("/rest/v1/sts/token"))
                .willReturn(okJson("""{
    "access_token": "test token",
    "token_type": "Bearer",
    "expires_in": 3600
}""")))
    }

    val stubbet_person = Person(
            id = AktørId("1078277661159"),
            fdato = parse("1970-09-01"),
            fornavn = "MAX",
            etternavn = "SMEKKER",
            kjønn = Kjønn.MANN,
            bostedsland = "NOR"
    )

    private fun personStub(aktørId: String) {
        stubFor(any(urlPathEqualTo("/api/person/$aktørId"))
                .willReturn(okJson(defaultObjectMapper.writeValueAsString(stubbet_person))))
    }

    private fun inntektStub(aktørId: String) {
        var inntekter: List<Inntekt> = List(14, init = { index ->
            Inntekt(
                    arbeidsgiver = Inntektsarbeidsgiver("97114455"),
                    beløp = BigDecimal.valueOf(25000),
                    opptjeningsperiode = Opptjeningsperiode(
                            fom = parse("2017-11-01").plusMonths(index.toLong()),
                            tom = parse("2017-11-30").plusMonths(index.toLong())
                    )
            )
        })

        stubFor(any(urlPathEqualTo("/api/inntekt/$aktørId/beregningsgrunnlag"))
                .willReturn(okJson(
                        defaultObjectMapper.writeValueAsString(InntektsoppslagResultat(inntekter))
                )))

        stubFor(any(urlPathEqualTo("/api/inntekt/$aktørId/sammenligningsgrunnlag"))
                .willReturn(okJson("""{
    "inntekter": [
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2019-01-31",
                "fom": "2019-01-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-12-31",
                "fom": "2018-12-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-11-30",
                "fom": "2018-11-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-10-31",
                "fom": "2018-10-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-09-30",
                "fom": "2018-09-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-08-31",
                "fom": "2018-08-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-07-31",
                "fom": "2018-07-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-06-30",
                "fom": "2018-06-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-05-31",
                "fom": "2018-05-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-04-30",
                "fom": "2018-04-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-03-31",
                "fom": "2018-03-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-02-28",
                "fom": "2018-02-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-01-31",
                "fom": "2018-01-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2017-12-31",
                "fom": "2017-12-01"
            }
        }
    ]
}""")))
    }

    val stubbet_arbeidsforhold = Arbeidsforhold(
            Arbeidsgiver(
                    navn = "EQUINOR ASA, AVD STATOIL SOKKELVIRKSOMHET",
                    orgnummer = "97114455"
            ),
            startdato = parse("2017-01-01"),
            sluttdato = null
    )
    private fun arbeidsforholdStub(aktørId: String) {

        val arbeidsforholdWrapper = ArbeidsforholdWrapper(
                arbeidsforhold = arrayOf(stubbet_arbeidsforhold)
        )

        stubFor(any(urlPathEqualTo("/api/arbeidsforhold/$aktørId"))
                .willReturn((okJson(defaultObjectMapper.writeValueAsString(arbeidsforholdWrapper)))))
    }
}
