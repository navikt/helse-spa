package no.nav.helse

import assertk.assert
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.each
import assertk.assertions.hasSize
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isNotIn
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.prometheus.client.CollectorRegistry
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.behandling.AvklarteVerdier
import no.nav.helse.behandling.Faktagrunnlag
import no.nav.helse.behandling.Fordeling
import no.nav.helse.behandling.Sakskompleks
import no.nav.helse.behandling.SykepengeVedtak
import no.nav.helse.behandling.Tpsfakta
import no.nav.helse.behandling.Vedtak
import no.nav.helse.behandling.Vedtaksperiode
import no.nav.helse.behandling.søknad.Sykepengesøknad
import no.nav.helse.dto.SykepengesøknadV2DTO
import no.nav.helse.fastsetting.Alder
import no.nav.helse.fastsetting.Aldersgrunnlag
import no.nav.helse.fastsetting.Opptjeningsgrunnlag
import no.nav.helse.fastsetting.Opptjeningstid
import no.nav.helse.fastsetting.Sykepengegrunnlag
import no.nav.helse.fastsetting.Vurdering
import no.nav.helse.fastsetting.begrunnelse_p_8_51
import no.nav.helse.fastsetting.begrunnelse_søker_i_aktivt_arbeidsforhold
import no.nav.helse.fastsetting.bosattstatus
import no.nav.helse.fastsetting.landskodeNORGE
import no.nav.helse.fastsetting.paragraf_8_28_andre_ledd
import no.nav.helse.fastsetting.paragraf_8_30_første_ledd
import no.nav.helse.fastsetting.søkerOppfyllerKravOmMedlemskap
import no.nav.helse.oppslag.AnvistPeriodeDTO
import no.nav.helse.oppslag.Inntekt
import no.nav.helse.oppslag.Inntektsarbeidsgiver
import no.nav.helse.oppslag.InntektsoppslagResultat
import no.nav.helse.oppslag.Kjønn
import no.nav.helse.oppslag.PersonDTO
import no.nav.helse.oppslag.StsRestClient
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidInntektYtelseDTO
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsforholdDTO
import no.nav.helse.oppslag.arbeidinntektytelse.dto.InntektDTO
import no.nav.helse.oppslag.arbeidinntektytelse.dto.InntektMedArbeidsforholdDTO
import no.nav.helse.oppslag.arbeidinntektytelse.dto.VirksomhetDTO
import no.nav.helse.oppslag.arbeidinntektytelse.dto.YtelserDTO
import no.nav.helse.serde.JsonNodeSerializer
import no.nav.helse.streams.Topics.SYKEPENGEBEHANDLINGSFEIL
import no.nav.helse.streams.Topics.SYKEPENGESØKNADER_INN
import no.nav.helse.streams.Topics.VEDTAK_SYKEPENGER
import no.nav.helse.streams.defaultObjectMapper
import no.nav.helse.sykepenger.beregning.Beregningsresultat
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDate.parse
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import java.util.concurrent.TimeUnit

class EndToEndTest {

    companion object {
        const val username = "srvkafkaclient"
        const val password = "kafkaclient"

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        val embeddedEnvironment = KafkaEnvironment(
            users = listOf(JAASCredential(username, password)),
            autoStart = false,
            withSchemaRegistry = false,
            withSecurity = true,
            topics = listOf(
                SYKEPENGESØKNADER_INN.name,
                VEDTAK_SYKEPENGER.name,
                SYKEPENGEBEHANDLINGSFEIL.name,
                SAKSKOMPLEKS_TOPIC
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
    fun `behandle et sakskompleks`() {
        val aktørId = "11987654321"

        println("Kafka: ${embeddedEnvironment.brokersURL}")
        println("Zookeeper: ${embeddedEnvironment.serverPark.zookeeper.host}:${embeddedEnvironment.serverPark.zookeeper.port}")

        restStsStub()
        personStub(aktørId)
        inntektStub(aktørId)
        arbeidsforholdStub(aktørId)
        sykepengehistorikkStub(aktørId)
        ytelserStub(aktørId)

        val sakskompleksJson = objectMapper.readTree("/sakskompleks/sakskompleks.json".readResource())
        val sakskompleks = Sakskompleks(sakskompleksJson)

        produceOneMessage(SAKSKOMPLEKS_TOPIC, sakskompleks.id, sakskompleks.jsonNode)

        val sykepengeVedtak: SykepengeVedtak = ventPåVedtak()

        //TODO checkSakskompleks()
        checkFaktagrunnlag(sykepengeVedtak.faktagrunnlag)
        checkAvklarteVerdier(sykepengeVedtak.faktagrunnlag, sykepengeVedtak.avklarteVerdier)
        checkVilkårsprøving(sykepengeVedtak.vilkårsprøving)
        checkBeregning(sykepengeVedtak.beregning)
        checkVedtak(sykepengeVedtak.vedtak)
    }

    private fun checkSøknad(innsendtSøknad: SykepengesøknadV2DTO, faktiskSøknad: Sykepengesøknad) {
        assert(innsendtSøknad.aktorId).isEqualTo(faktiskSøknad.aktorId)
        assert(innsendtSøknad.status.name).isEqualTo(faktiskSøknad.status)
        assert(innsendtSøknad.arbeidsgiver.orgnummer).isEqualTo(faktiskSøknad.arbeidsgiver.orgnummer)
        assert(innsendtSøknad.soktUtenlandsopphold).isEqualTo(faktiskSøknad.soktUtenlandsopphold)
        assert(innsendtSøknad.fom).isEqualTo(første_dag_i_syketilfelle)
        assert(innsendtSøknad.tom).isEqualTo(faktiskSøknad.tom)
        assert(innsendtSøknad.startSyketilfelle).isEqualTo(første_dag_i_syketilfelle)
        assert(innsendtSøknad.sendtNav).isEqualTo(faktiskSøknad.sendtNav)

        innsendtSøknad.soknadsperioder.forEachIndexed { index, soknadsperiodeDTO ->
            assert(soknadsperiodeDTO.fom).isEqualTo(faktiskSøknad.soknadsperioder[index].fom)
            assert(soknadsperiodeDTO.tom).isEqualTo(faktiskSøknad.soknadsperioder[index].tom)
            assert(soknadsperiodeDTO.sykmeldingsgrad).isEqualTo(faktiskSøknad.soknadsperioder[index].sykmeldingsgrad)
        }
    }

    private fun checkFaktagrunnlag(faktagrunnlag: Faktagrunnlag) {
        checkTpsFakta(faktagrunnlag.tps)
        checkInntekt(
            faktagrunnlag.beregningsperiode,
            beregningsgrunnlagStart,
            beregningsgrunnlagStart.plusMonths(2).with(lastDayOfMonth())
        )
        checkInntekt(
            faktagrunnlag.sammenligningsperiode,
            sammenligningsgrunnlagStart,
            sammenligningsgrunnlagStart.plusMonths(11).with(lastDayOfMonth())
        )
        checkSykepengeliste(faktagrunnlag.sykepengehistorikk)
        checkArbeidsforhold(faktagrunnlag.arbeidInntektYtelse.arbeidsforhold)
    }

    private fun checkTpsFakta(tps: Tpsfakta) {
        assert(tps.bostedland).isEqualTo(stubbet_person.bostedsland)
        assert(tps.fodselsdato).isEqualTo(stubbet_person.fdato)
    }

    private fun checkAvklarteVerdier(faktagrunnlag: Faktagrunnlag, avklarteVerdier: AvklarteVerdier) {
        checkAlder(avklarteVerdier.alder)
        checkMaksdato(avklarteVerdier.alder.fastsattVerdi, faktagrunnlag.sykepengehistorikk, avklarteVerdier.maksdato)
        checkMedlemsskap(avklarteVerdier.medlemsskap)
        checkSykepengegrunnlag(avklarteVerdier.sykepengegrunnlag)
        checkArbeidsforholdVurdering(avklarteVerdier.arbeidsforhold)
        checkOpptjeningstid(avklarteVerdier.opptjeningstid)
        checkSykepengeliste(avklarteVerdier.sykepengehistorikk)
    }

    private fun checkAlder(aldersVurdering: Vurdering.Avklart<Alder, Aldersgrunnlag>) {
        assert(aldersVurdering.fastsattVerdi).isEqualTo(48)
        assert(aldersVurdering.begrunnelse).isEqualTo(begrunnelse_p_8_51)
        assert(aldersVurdering.vurderingstidspunkt).isBetween(
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1)
        )
        assert(aldersVurdering.fastsattAv).isEqualTo("SPA")

        assert(aldersVurdering.grunnlag.fodselsdato).isEqualTo(stubbet_person.fdato)
    }

    private fun checkMaksdato(
        alder: Alder,
        sykepengehistorikk: List<AnvistPeriodeDTO>,
        maksdato: Vurdering.Avklart<LocalDate, Grunnlagsdata>
    ) {
        val forventetMaksdato = LocalDate.of(2019, 11, 28)

        assert(maksdato.fastsattVerdi).isEqualTo(forventetMaksdato)
        assert(maksdato.begrunnelse).isEqualTo("§ 8-12: ARBEIDSTAKER på 48 år gir maks 248 dager. 10 av disse er forbrukt")
        assert(maksdato.vurderingstidspunkt).isBetween(
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1)
        )
        assert(maksdato.fastsattAv).isEqualTo("SPA")

        val grunnlag = maksdato.grunnlag
        assert(grunnlag.førsteFraværsdag).isEqualTo(første_dag_i_syketilfelle)
        assert(grunnlag.førsteSykepengedag).isEqualTo(første_dag_i_syketilfelle)
        assert(grunnlag.personensAlder).isEqualTo(alder)
        assert(grunnlag.yrkesstatus).isEqualTo(Yrkesstatus.ARBEIDSTAKER)
        assert(grunnlag.tidligerePerioder).isEqualTo(sykepengehistorikk.map { Tidsperiode(it.fom, it.tom) })
    }

    private fun checkMedlemsskap(medlemsskap: Vurdering.Avklart<Boolean, Tpsfakta>) {
        assert(medlemsskap.fastsattVerdi).isTrue()
        assert(medlemsskap.begrunnelse).contains(søkerOppfyllerKravOmMedlemskap)
        assert(medlemsskap.vurderingstidspunkt).isBetween(
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1)
        )
        assert(medlemsskap.fastsattAv).isEqualTo("SPA")

        assert(medlemsskap.grunnlag.bostedland).isEqualTo(landskodeNORGE)
        assert(medlemsskap.grunnlag.statsborgerskap).isEqualTo(landskodeNORGE)
        assert(medlemsskap.grunnlag.status).isEqualTo(bosattstatus)
        assert(medlemsskap.grunnlag.diskresjonskode).isNull()
    }

    private fun checkSykepengegrunnlag(sykepengegrunnlagVurdering: Vurdering.Avklart<Sykepengegrunnlag, List<Inntekt>>) {
        checkSykepengegrunnlagNårTrygdenYter(sykepengegrunnlagVurdering.fastsattVerdi.sykepengegrunnlagNårTrygdenYter)
        checkSykepengegrunnlagIArbeidsgiverperioden(sykepengegrunnlagVurdering.fastsattVerdi.sykepengegrunnlagIArbeidsgiverperioden)
        assert(sykepengegrunnlagVurdering.begrunnelse).isEqualTo("")
        assert(sykepengegrunnlagVurdering.vurderingstidspunkt).isBetween(
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1)
        )
        assert(sykepengegrunnlagVurdering.fastsattAv).isEqualTo("SPA")

        checkBeregningsperiode12mnd(sykepengegrunnlagVurdering.grunnlag)
    }

    private fun checkSykepengegrunnlagNårTrygdenYter(sykepengegrunnlagNårTrygdenYterVurdering: Vurdering.Avklart<Long, List<Inntekt>>) {
        assert(sykepengegrunnlagNårTrygdenYterVurdering.fastsattVerdi).isEqualTo(300000L)
        assert(sykepengegrunnlagNårTrygdenYterVurdering.begrunnelse).contains(paragraf_8_30_første_ledd)
        assert(sykepengegrunnlagNårTrygdenYterVurdering.vurderingstidspunkt).isBetween(
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1)
        )
        assert(sykepengegrunnlagNårTrygdenYterVurdering.fastsattAv).isEqualTo("SPA")

        checkBeregningsperiode3mnd(sykepengegrunnlagNårTrygdenYterVurdering.grunnlag)
    }

    private fun checkSykepengegrunnlagIArbeidsgiverperioden(sykepengegrunnlagIArbeidsgiverperiodenVurdering: Vurdering.Avklart<Long, List<Inntekt>>) {
        assert(sykepengegrunnlagIArbeidsgiverperiodenVurdering.fastsattVerdi).isEqualTo(25000L)
        assert(sykepengegrunnlagIArbeidsgiverperiodenVurdering.begrunnelse).contains(paragraf_8_28_andre_ledd)
        assert(sykepengegrunnlagIArbeidsgiverperiodenVurdering.vurderingstidspunkt).isBetween(
            LocalDateTime.now().minusHours(
                1
            ), LocalDateTime.now().plusHours(1)
        )
        assert(sykepengegrunnlagIArbeidsgiverperiodenVurdering.fastsattAv).isEqualTo("SPA")

        checkBeregningsperiode3mnd(sykepengegrunnlagIArbeidsgiverperiodenVurdering.grunnlag)
    }

    private fun checkBeregningsperiode3mnd(beregningsperiode: List<Inntekt>) {
        checkInntekt(
            beregningsperiode,
            beregningsgrunnlagStart,
            beregningsgrunnlagStart.plusMonths(2).with(lastDayOfMonth())
        )
    }

    private fun checkBeregningsperiode12mnd(beregningsperiode: List<Inntekt>) {
        checkInntekt(
            beregningsperiode,
            sammenligningsgrunnlagStart,
            sammenligningsgrunnlagStart.plusMonths(11).with(lastDayOfMonth())
        )
    }

    private fun checkInntekt(inntekter: List<Inntekt>, startDate: LocalDate, endDate: LocalDate) {
        assert(inntekter).hasSize(startDate.until(endDate.plusDays(1), ChronoUnit.MONTHS).toInt())
        assert(inntekter).each {
            val inntekt = it.actual
            assert(inntekt.virksomhet.identifikator).isEqualTo(stubbet_arbeidsforhold.arbeidsgiver.identifikator)
            assert(inntekt.beløp.toLong()).isEqualTo(25000L)
            assert(inntekt.utbetalingsperiode).isBetween(YearMonth.from(startDate), YearMonth.from(endDate))
        }
    }

    private fun checkArbeidsforholdVurdering(arbeidsforholdVurdering: Vurdering.Avklart<ArbeidsforholdDTO, List<ArbeidsforholdDTO>>) {
        assert(arbeidsforholdVurdering.fastsattVerdi).isEqualTo(stubbet_arbeidsforhold)
        assert(arbeidsforholdVurdering.begrunnelse).isEqualTo("Søker har et arbeidsforhold hos MATBUTIKKEN")
        assert(arbeidsforholdVurdering.vurderingstidspunkt).isBetween(
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1)
        )
        assert(arbeidsforholdVurdering.fastsattAv).isEqualTo("SPA")

        checkArbeidsforhold(arbeidsforholdVurdering.grunnlag)
    }

    private fun checkArbeidsforhold(arbeidsforhold: List<ArbeidsforholdDTO>) {
        assert(arbeidsforhold).containsExactly(stubbet_arbeidsforhold)
    }

    private fun checkOpptjeningstid(opptjeningstidVurdering: Vurdering.Avklart<Opptjeningstid, Opptjeningsgrunnlag>) {
        assert(opptjeningstidVurdering.fastsattVerdi).isEqualTo(730L) // Antall dager fra arbeidsfrholdets startdato til første sykdomsdag
        assert(opptjeningstidVurdering.begrunnelse).isEqualTo(begrunnelse_søker_i_aktivt_arbeidsforhold)
        assert(opptjeningstidVurdering.vurderingstidspunkt).isBetween(
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1)
        )
        assert(opptjeningstidVurdering.fastsattAv).isEqualTo("SPA")

        val opptjeningsgrunnlag = opptjeningstidVurdering.grunnlag
        assert(opptjeningsgrunnlag.førsteSykdomsdag).isEqualTo(første_dag_i_syketilfelle)
        checkArbeidsforholdVurdering((opptjeningsgrunnlag.arbeidsforhold as Vurdering.Avklart))
    }

    private fun checkSykepengeliste(sykepengeliste: List<AnvistPeriodeDTO>) {
        assert(sykepengeliste).isEqualTo(tiDagerSykepengeHistorikk())
    }

    private fun checkVilkårsprøving(vilkårsprøving: Evaluering) {
        // Detaljene i vilkårsprøving testes ikke her, men resultatet for dette caset skal være JA
        assert(vilkårsprøving.resultat).isEqualTo(Resultat.JA)
    }

    private fun checkBeregning(beregning: Beregningsresultat) {
        // Detaljene i beregningen testes ikke her, men resultatet for dette caset skal være 23 virkedager a 1154,-
        assert(beregning.dagsatser).hasSize(11)
        assert(beregning.dagsatser).each {
            val dagsats = it.actual
            assert(dagsats.dato).isBetween(første_dag_i_syketilfelle, siste_dag_i_syketilfelle)
            assert(dagsats.dato.dayOfWeek).isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            assert(dagsats.sats).isEqualTo(1154L)
            assert(dagsats.skalUtbetales).isTrue()
        }
    }

    private fun checkVedtak(vedtak: Vedtak) {
        assert(vedtak.perioder).containsExactly(
            Vedtaksperiode(
                fom = LocalDate.of(2019, 1, 17),
                tom = LocalDate.of(2019, 1, 31),
                dagsats = BigDecimal.valueOf(1154L),
                fordeling = listOf(Fordeling(mottager = "97114455", andel = 100))
            )
        )
    }

    val første_dag_i_syketilfelle = parse("2019-01-01")
    val siste_dag_i_syketilfelle = parse("2019-01-31")

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

    class BehandlingsfeilDeserializer : Deserializer<Behandlingsfeil> {
        private val log = LoggerFactory.getLogger("BehandlingsfeilDeserializer")

        override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}

        override fun deserialize(topic: String?, data: ByteArray?): Behandlingsfeil? {
            return data?.let {
                try {
                    defaultObjectMapper.readValue<Behandlingsfeil.MVPFilterFeil>(it)
                } catch (e: Exception) {
                    log.warn("Not a valid json", e)
                    null
                }
            }
        }

        override fun close() {}

    }

    private fun ventPåVedtak(): SykepengeVedtak {
        val resultConsumer = KafkaConsumer<String, SykepengeVedtak>(
            consumerProperties(),
            StringDeserializer(),
            SykepengeVedtakDeserializer()
        )
        resultConsumer.subscribe(listOf(VEDTAK_SYKEPENGER.name))

        val end = System.currentTimeMillis() + 20 * 1000

        while (System.currentTimeMillis() < end) {
            resultConsumer.seekToBeginning(resultConsumer.assignment())
            val records = resultConsumer.poll(Duration.ofSeconds(1))

            if (!records.isEmpty) {
                return records.records(VEDTAK_SYKEPENGER.name).map {
                    it.value()
                }.first()
            }
        }

        throw RuntimeException("fant ingen vedtak etter 20 sekunder")
    }

    private fun ventPåBehandlingsfeil(): Behandlingsfeil {
        val resultConsumer = KafkaConsumer<String, Behandlingsfeil>(
            consumerProperties(),
            StringDeserializer(),
            BehandlingsfeilDeserializer()
        )
        resultConsumer.subscribe(listOf(SYKEPENGEBEHANDLINGSFEIL.name))

        val end = System.currentTimeMillis() + 20 * 1000

        while (System.currentTimeMillis() < end) {
            resultConsumer.seekToBeginning(resultConsumer.assignment())
            val records = resultConsumer.poll(Duration.ofSeconds(1))

            if (!records.isEmpty) {
                return records.records(SYKEPENGEBEHANDLINGSFEIL.name).map {
                    it.value()
                }.first()
            }
        }

        throw RuntimeException("fant ingen behandlingsfeil etter 20 sekunder")
    }

    private fun restStsStub() {
        stubFor(
            any(urlPathEqualTo("/rest/v1/sts/token"))
                .willReturn(
                    okJson(
                        defaultObjectMapper.writeValueAsString(
                            StsRestClient.Token(
                                accessToken = "test token",
                                type = "Bearer",
                                expiresIn = 3600
                            )
                        )
                    )
                )
        )
    }

    val stubbet_person = PersonDTO(
        aktørId = "1078277661159",
        fdato = parse("1970-09-01"),
        fornavn = "MAX",
        etternavn = "SMEKKER",
        kjønn = Kjønn.MANN,
        bostedsland = "NOR",
        statsborgerskap = "NOR",
        status = "BOSA",
        diskresjonskode = null
    )

    val stubbet_arbeidsforhold = ArbeidsforholdDTO(
        type = "Arbeidstaker",
        arbeidsgiver = no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsgiverDTO(
            type = "Organisasjon",
            identifikator = "97114455"
        ),
        startdato = parse("2017-01-01"),
        sluttdato = null
    )

    val beregningsgrunnlagStart = første_dag_i_syketilfelle.minusMonths(3)
    val stubbet_inntekt_beregningsgrunnlag: List<Inntekt> = List(3, init = { index ->
        Inntekt(
            virksomhet = Inntektsarbeidsgiver(stubbet_arbeidsforhold.arbeidsgiver.identifikator, "Organisasjon"),
            beløp = BigDecimal.valueOf(25000),
            utbetalingsperiode = YearMonth.from(beregningsgrunnlagStart.plusMonths(index.toLong())),
            type = "Lønn",
            ytelse = false,
            kode = null
        )
    })

    val sammenligningsgrunnlagStart = første_dag_i_syketilfelle.minusMonths(12)
    val stubbet_inntekt_sammenligningsgrunnlag: List<Inntekt> = List(12, init = { index ->
        Inntekt(
            virksomhet = Inntektsarbeidsgiver(stubbet_arbeidsforhold.arbeidsgiver.identifikator, "Organisasjon"),
            beløp = BigDecimal.valueOf(25000),
            utbetalingsperiode = YearMonth.from(sammenligningsgrunnlagStart.plusMonths(index.toLong())),
            type = "Lønn",
            ytelse = false,
            kode = null
        )
    })

    private fun personStub(aktørId: String) {
        stubFor(
            any(urlPathEqualTo("/api/person/$aktørId"))
                .willReturn(okJson(defaultObjectMapper.writeValueAsString(stubbet_person)))
        )
    }

    private fun inntektStub(aktørId: String) {
        stubFor(
            any(urlPathEqualTo("/api/inntekt/$aktørId/beregningsgrunnlag/${stubbet_arbeidsforhold.arbeidsgiver.identifikator}"))
                .willReturn(
                    okJson(
                        defaultObjectMapper.writeValueAsString(
                            InntektsoppslagResultat(stubbet_inntekt_beregningsgrunnlag)
                        )
                    )
                )
        )

        stubFor(
            any(urlPathEqualTo("/api/inntekt/$aktørId/sammenligningsgrunnlag"))
                .willReturn(
                    okJson(
                        defaultObjectMapper.writeValueAsString(
                            InntektsoppslagResultat(stubbet_inntekt_sammenligningsgrunnlag)
                        )
                    )
                )
        )
    }

    private fun ytelserStub(aktørId: String) {
        stubFor(
            any(urlPathEqualTo("/api/ytelser/$aktørId"))
                .willReturn(
                    okJson(
                        defaultObjectMapper.writeValueAsString(
                            YtelserDTO(
                                infotrygd = emptyList(),
                                arena = emptyList()
                            )
                        )
                    )
                )
        )
    }

    private fun sykepengehistorikkStub(aktørId: String) {
        stubFor(
            any(urlPathEqualTo("/api/sykepengehistorikk/$aktørId"))
                .willReturn(
                    okJson(
                        defaultObjectMapper.writeValueAsString(
                            tiDagerSykepengeHistorikk()
                        )
                    )
                )
        )
    }

    private fun tiDagerSykepengeHistorikk(): List<AnvistPeriodeDTO> {
        val someMonday = første_dag_i_syketilfelle.minusMonths(1).with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        return listOf(AnvistPeriodeDTO(someMonday, someMonday.plusDays(13)))
    }

    private fun arbeidsforholdStub(aktørId: String) {

        val arbeidsforholdWrapper = ArbeidInntektYtelseDTO(
            arbeidsforhold = listOf(stubbet_arbeidsforhold),
            inntekter = listOf(
                InntektMedArbeidsforholdDTO(
                    inntekt = InntektDTO(
                        virksomhet = VirksomhetDTO(
                            stubbet_arbeidsforhold.arbeidsgiver.identifikator,
                            stubbet_arbeidsforhold.arbeidsgiver.type
                        ),
                        utbetalingsperiode = YearMonth.now(),
                        beløp = BigDecimal.ONE
                    ),
                    muligeArbeidsforhold = listOf(stubbet_arbeidsforhold)
                )
            ),
            ytelser = emptyList()
        )

        stubFor(
            any(urlPathEqualTo("/api/arbeidsforhold/$aktørId/inntekter"))
                .willReturn((okJson(defaultObjectMapper.writeValueAsString(arbeidsforholdWrapper))))
        )
    }

    private fun produceOneMessage(topic: String, key: String, message: JsonNode) {
        val producer =
            KafkaProducer<String, JsonNode>(producerProperties(), StringSerializer(), JsonNodeSerializer(objectMapper))
        producer.send(ProducerRecord(topic, key, message))
            .get(1, TimeUnit.SECONDS)
        producer.flush()
    }

    private fun getCounterValue(name: String, labelValues: List<String> = emptyList()) =
        (CollectorRegistry.defaultRegistry
            .findMetricSample(name, labelValues)
            ?.value ?: 0.0).toInt()

    private fun CollectorRegistry.findMetricSample(name: String, labelValues: List<String>) =
        findSamples(name).firstOrNull { sample ->
            sample.labelValues.size == labelValues.size && sample.labelValues.containsAll(labelValues)
        }

    private fun CollectorRegistry.findSamples(name: String) =
        filteredMetricFamilySamples(setOf(name))
            .toList()
            .flatMap { metricFamily ->
                metricFamily.samples
            }
}

private fun consumerProperties(): MutableMap<String, Any>? {
    return HashMap<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, EndToEndTest.embeddedEnvironment.brokersURL)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(
            SaslConfigs.SASL_JAAS_CONFIG,
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${EndToEndTest.username}\" password=\"${EndToEndTest.password}\";"
        )
        put(ConsumerConfig.GROUP_ID_CONFIG, "spa-e2e-verification")
    }
}

private fun producerProperties(): MutableMap<String, Any>? {
    return HashMap<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, EndToEndTest.embeddedEnvironment.brokersURL)
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(
            SaslConfigs.SASL_JAAS_CONFIG,
            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${EndToEndTest.username}\" password=\"${EndToEndTest.password}\";"
        )
    }
}
