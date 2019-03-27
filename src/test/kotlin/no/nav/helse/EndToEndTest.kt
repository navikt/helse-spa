package no.nav.helse

import assertk.assert
import assertk.assertions.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
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
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters.firstDayOfMonth
import java.time.temporal.TemporalAdjusters.lastDayOfMonth

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
        checkFaktagrunnlag(vedtak.faktagrunnlag)
        checkAvklarteVerdier(vedtak.faktagrunnlag, vedtak.avklarteVerdier)
        checkVilkårsprøving(vedtak.vilkårsprøving)
        // TODO       checkBeregning(vedtak.beregning)
        // TODO       checkSykepengeVedtak(vedtak.vedtak)
    }

    private fun checkSøknad(innsendtSøknad: SykepengesøknadV2DTO, faktiskSøknad: Sykepengesøknad) {
        assert(innsendtSøknad.aktorId).isEqualTo(faktiskSøknad.aktorId)
        assert(innsendtSøknad.status).isEqualTo(faktiskSøknad.status)
        assert(innsendtSøknad.arbeidsgiver).isEqualTo(faktiskSøknad.arbeidsgiver)
        assert(innsendtSøknad.soktUtenlandsopphold).isEqualTo(faktiskSøknad.soktUtenlandsopphold)
        assert(innsendtSøknad.fom).isEqualTo(første_dag_i_syketilfelle)
        assert(innsendtSøknad.tom).isEqualTo(faktiskSøknad.tom)
        assert(innsendtSøknad.startSyketilfelle).isEqualTo(første_dag_i_syketilfelle)
        assert(innsendtSøknad.sendtNav).isEqualTo(faktiskSøknad.sendtNav)
        assert(innsendtSøknad.soknadsperioder).isEqualTo(faktiskSøknad.soknadsperioder)
    }

    private fun checkFaktagrunnlag(faktagrunnlag: Faktagrunnlag) {
        checkTpsFakta(faktagrunnlag.tps)
        checkInntekt(faktagrunnlag.beregningsperiode, beregningsgrunnlagStart, beregningsgrunnlagStart.plusMonths(2).with(lastDayOfMonth()))
        checkInntekt(faktagrunnlag.sammenligningsperiode, sammenligningsgrunnlagStart, sammenligningsgrunnlagStart.plusMonths(11).with(lastDayOfMonth()))
        checkSykepengeliste(faktagrunnlag.sykepengeliste)
        checkArbeidsforhold(faktagrunnlag.arbeidsforhold)
    }

    private fun checkTpsFakta(tps: Tpsfakta) {
        assert(tps.bostedland).isEqualTo(stubbet_person.bostedsland)
        assert(tps.fodselsdato).isEqualTo(stubbet_person.fdato)
    }

    private fun checkAvklarteVerdier(faktagrunnlag: Faktagrunnlag, avklarteVerdier: AvklarteVerdier) {
        checkAlder(avklarteVerdier.alder)
        checkMaksdato(avklarteVerdier.alder.fastsattVerdi, faktagrunnlag.sykepengeliste, avklarteVerdier.maksdato)
        checkMedlemsskap(avklarteVerdier.medlemsskap)
        checkSykepengegrunnlag(avklarteVerdier.sykepengegrunnlag)
        checkArbeidsforholdVurdering(avklarteVerdier.arbeidsforhold)
        checkOpptjeningstid(avklarteVerdier.opptjeningstid)
        checkSykepengeliste(avklarteVerdier.sykepengeliste)
    }

    private fun checkAlder(aldersVurdering: Vurdering.Avklart<Alder, Aldersgrunnlag>) {
        assert(aldersVurdering.fastsattVerdi).isEqualTo(48)
        assert(aldersVurdering.begrunnelse).isEqualTo(begrunnelse_p_8_51)
        assert(aldersVurdering.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(aldersVurdering.fastsattAv).isEqualTo("SPA")

        assert(aldersVurdering.grunnlag.fodselsdato).isEqualTo(stubbet_person.fdato)
    }

    private fun checkMaksdato(alder: Alder, sykepengeListe: Collection<SykepengerPeriode>, maksdato: Vurdering.Avklart<LocalDate, Grunnlagsdata>) {
        val forventetMaksdato = LocalDate.of(2019, 12, 12)

        assert(maksdato.fastsattVerdi).isEqualTo(forventetMaksdato)
        assert(maksdato.begrunnelse).isEqualTo("§ 8-12: ARBEIDSTAKER på 48 år gir maks 248 dager. 0 av disse er forbrukt")
        assert(maksdato.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(maksdato.fastsattAv).isEqualTo("SPA")

        val grunnlag = maksdato.grunnlag
        assert(grunnlag.førsteFraværsdag).isEqualTo(første_dag_i_syketilfelle)
        assert(grunnlag.førsteSykepengedag).isEqualTo(første_dag_i_syketilfelle)
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

    private fun checkSykepengegrunnlag(sykepengegrunnlagVurdering: Vurdering.Avklart<Sykepengegrunnlag, Beregningsperiode>) {
        checkSykepengegrunnlagNårTrygdenYter(sykepengegrunnlagVurdering.fastsattVerdi.sykepengegrunnlagNårTrygdenYter)
        checkSykepengegrunnlagIArbeidsgiverperioden(sykepengegrunnlagVurdering.fastsattVerdi.sykepengegrunnlagIArbeidsgiverperioden)
        assert(sykepengegrunnlagVurdering.begrunnelse).isEqualTo("")
        assert(sykepengegrunnlagVurdering.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(sykepengegrunnlagVurdering.fastsattAv).isEqualTo("SPA")

        checkBeregningsperiode12mnd(sykepengegrunnlagVurdering.grunnlag)
    }

    private fun checkSykepengegrunnlagNårTrygdenYter(sykepengegrunnlagNårTrygdenYterVurdering: Vurdering.Avklart<Long, Beregningsperiode>) {
        assert(sykepengegrunnlagNårTrygdenYterVurdering.fastsattVerdi).isEqualTo(300000L)
        assert(sykepengegrunnlagNårTrygdenYterVurdering.begrunnelse).contains(paragraf_8_30_første_ledd)
        assert(sykepengegrunnlagNårTrygdenYterVurdering.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(sykepengegrunnlagNårTrygdenYterVurdering.fastsattAv).isEqualTo("SPA")

        checkBeregningsperiode3mnd(sykepengegrunnlagNårTrygdenYterVurdering.grunnlag)
    }

    private fun checkSykepengegrunnlagIArbeidsgiverperioden(sykepengegrunnlagIArbeidsgiverperiodenVurdering: Vurdering.Avklart<Long, Beregningsperiode>) {
        assert(sykepengegrunnlagIArbeidsgiverperiodenVurdering.fastsattVerdi).isEqualTo(25000L)
        assert(sykepengegrunnlagIArbeidsgiverperiodenVurdering.begrunnelse).contains(paragraf_8_28_andre_ledd)
        assert(sykepengegrunnlagIArbeidsgiverperiodenVurdering.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(sykepengegrunnlagIArbeidsgiverperiodenVurdering.fastsattAv).isEqualTo("SPA")

        checkBeregningsperiode3mnd(sykepengegrunnlagIArbeidsgiverperiodenVurdering.grunnlag)
    }

    private fun checkBeregningsperiode3mnd(beregningsperiode: Beregningsperiode) {
        assert(beregningsperiode.begrunnelse).isEqualTo(paragraf_8_28_tredje_ledd_bokstav_a + "(${første_dag_i_syketilfelle}) legges til grunn.")

        checkInntekt(beregningsperiode.inntekter, beregningsgrunnlagStart, beregningsgrunnlagStart.plusMonths(2).with(lastDayOfMonth()))
    }

    private fun checkBeregningsperiode12mnd(beregningsperiode: Beregningsperiode) {
        assert(beregningsperiode.begrunnelse).isNotEmpty()

        checkInntekt(beregningsperiode.inntekter, sammenligningsgrunnlagStart, sammenligningsgrunnlagStart.plusMonths(11).with(lastDayOfMonth()))
    }

    private fun checkInntekt(inntekter: List<Inntekt>, startDate: LocalDate, endDate: LocalDate) {
        assert(inntekter).hasSize(startDate.until(endDate.plusDays(1), ChronoUnit.MONTHS).toInt())
        assert(inntekter).each {
            val inntekt = it.actual
            assert(inntekt.arbeidsgiver.orgnr).isEqualTo(stubbet_arbeidsforhold.arbeidsgiver.orgnummer)
            assert(inntekt.beløp.toLong()).isEqualTo(25000L)
            assert(inntekt.opptjeningsperiode.fom).isBetween(startDate, endDate.with(firstDayOfMonth()))
            assert(inntekt.opptjeningsperiode.tom).isBetween(startDate.with(lastDayOfMonth()), endDate)
        }
    }

    private fun checkArbeidsforholdVurdering(arbeidsforholdVurdering: Vurdering.Avklart<Boolean, List<Arbeidsforhold>>) {
        assert(arbeidsforholdVurdering.fastsattVerdi).isTrue()
        assert(arbeidsforholdVurdering.begrunnelse).isEqualTo(søker_har_arbeidsgiver + stubbet_arbeidsforhold.arbeidsgiver.orgnummer)
        assert(arbeidsforholdVurdering.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(arbeidsforholdVurdering.fastsattAv).isEqualTo("SPA")

        checkArbeidsforhold(arbeidsforholdVurdering.grunnlag)
    }

    private fun checkArbeidsforhold(arbeidsforhold: List<Arbeidsforhold>) {
        assert(arbeidsforhold).containsExactly(stubbet_arbeidsforhold)
    }

    private fun checkOpptjeningstid(opptjeningstidVurdering: Vurdering.Avklart<Opptjeningstid, Opptjeningsgrunnlag>) {
        assert(opptjeningstidVurdering.fastsattVerdi).isEqualTo(730L) // Antall dager fra arbeidsfrholdets startdato til første sykdomsdag
        assert(opptjeningstidVurdering.begrunnelse).isEqualTo(begrunnelse_søker_i_aktivt_arbeidsforhold)
        assert(opptjeningstidVurdering.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(opptjeningstidVurdering.fastsattAv).isEqualTo("SPA")

        val opptjeningsgrunnlag = opptjeningstidVurdering.grunnlag
        assert(opptjeningsgrunnlag.førsteSykdomsdag).isEqualTo(første_dag_i_syketilfelle)
        checkArbeidsforhold(opptjeningsgrunnlag.arbeidsforhold)
    }

    private fun checkSykepengeliste(sykepengeliste: Collection<SykepengerPeriode>) {
        assert(sykepengeliste).isEmpty()
    }

    private fun checkVilkårsprøving(vilkårsprøving: Evaluering) {
        assert(vilkårsprøving.resultat).isEqualTo(Resultat.JA)
    }

    private fun checkBeregning(beregning: Beregningsresultat) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun checkSykepengeVedtak(vedtak: Vedtak) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    val første_dag_i_syketilfelle = parse("2019-01-01")

    private fun produserSykepengesøknadV2(aktørId: String): SykepengesøknadV2DTO {
        val søknad = SykepengesøknadV2DTO(
                id = "1",
                aktorId = aktørId,
                type = "ARBEIDSTAKERE",
                status = "SENDT",
                arbeidsgiver = stubbet_arbeidsforhold.arbeidsgiver,
                fom = første_dag_i_syketilfelle,
                tom = parse("2019-01-31"),
                startSyketilfelle = første_dag_i_syketilfelle,
                sendtNav = parse("2019-01-17").atStartOfDay(),
                soknadsperioder = listOf(
                        Soknadsperiode(
                                fom = første_dag_i_syketilfelle,
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

    val stubbet_arbeidsforhold = Arbeidsforhold(
            Arbeidsgiver(
                    navn = "EQUINOR ASA, AVD STATOIL SOKKELVIRKSOMHET",
                    orgnummer = "97114455"
            ),
            startdato = parse("2017-01-01"),
            sluttdato = null
    )

    val beregningsgrunnlagStart = første_dag_i_syketilfelle.minusMonths(3)
    val stubbet_inntekt_beregningsgrunnlag: List<Inntekt> = List(3, init = { index ->
        Inntekt(
                arbeidsgiver = Inntektsarbeidsgiver(stubbet_arbeidsforhold.arbeidsgiver.orgnummer),
                beløp = BigDecimal.valueOf(25000),
                opptjeningsperiode = Opptjeningsperiode(
                        fom = beregningsgrunnlagStart.plusMonths(index.toLong()),
                        tom = beregningsgrunnlagStart.plusMonths(index.toLong()).with(lastDayOfMonth())
                )
        )
    })

    val sammenligningsgrunnlagStart = første_dag_i_syketilfelle.minusMonths(12)
    val stubbet_inntekt_sammenligningsgrunnlag: List<Inntekt> = List(12, init = { index ->
        Inntekt(
                arbeidsgiver = Inntektsarbeidsgiver(stubbet_arbeidsforhold.arbeidsgiver.orgnummer),
                beløp = BigDecimal.valueOf(25000),
                opptjeningsperiode = Opptjeningsperiode(
                        fom = sammenligningsgrunnlagStart.plusMonths(index.toLong()),
                        tom = sammenligningsgrunnlagStart.plusMonths(index.toLong()).with(lastDayOfMonth())
                )
        )
    })

    private fun personStub(aktørId: String) {
        stubFor(any(urlPathEqualTo("/api/person/$aktørId"))
                .willReturn(okJson(defaultObjectMapper.writeValueAsString(stubbet_person))))
    }

    private fun inntektStub(aktørId: String) {
        stubFor(any(urlPathEqualTo("/api/inntekt/$aktørId/beregningsgrunnlag"))
                .willReturn(okJson(defaultObjectMapper.writeValueAsString(
                        InntektsoppslagResultat(stubbet_inntekt_beregningsgrunnlag)
                ))))

        stubFor(any(urlPathEqualTo("/api/inntekt/$aktørId/sammenligningsgrunnlag"))
                .willReturn(okJson(defaultObjectMapper.writeValueAsString(
                        InntektsoppslagResultat(stubbet_inntekt_sammenligningsgrunnlag)
                ))))
    }

    private fun arbeidsforholdStub(aktørId: String) {

        val arbeidsforholdWrapper = ArbeidsforholdWrapper(
                arbeidsforhold = arrayOf(stubbet_arbeidsforhold)
        )

        stubFor(any(urlPathEqualTo("/api/arbeidsforhold/$aktørId"))
                .willReturn((okJson(defaultObjectMapper.writeValueAsString(arbeidsforholdWrapper)))))
    }
}
