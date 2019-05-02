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
import no.nav.helse.domain.ArbeidsforholdMedInntekter
import no.nav.helse.domain.ArbeidsforholdWrapper
import no.nav.helse.domain.Arbeidsgiver
import no.nav.helse.dto.*
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
import java.time.*
import java.time.LocalDate.parse
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
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
        sykepengehistorikkStub(aktørId)

        val innsendtSøknad = produserSykepengesøknadV2(aktørId)

        val sykepengeVedtak: SykepengeVedtak = ventPåVedtak()

        checkSøknad(innsendtSøknad, sykepengeVedtak.originalSøknad)
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
        checkInntekt(faktagrunnlag.beregningsperiode, beregningsgrunnlagStart, beregningsgrunnlagStart.plusMonths(2).with(lastDayOfMonth()))
        checkInntekt(faktagrunnlag.sammenligningsperiode, sammenligningsgrunnlagStart, sammenligningsgrunnlagStart.plusMonths(11).with(lastDayOfMonth()))
        checkSykepengeliste(faktagrunnlag.sykepengehistorikk)
        checkArbeidsforhold(faktagrunnlag.arbeidsforhold)
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
        assert(aldersVurdering.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(aldersVurdering.fastsattAv).isEqualTo("SPA")

        assert(aldersVurdering.grunnlag.fodselsdato).isEqualTo(stubbet_person.fdato)
    }

    private fun checkMaksdato(alder: Alder, sykepengehistorikk: List<AnvistPeriode>, maksdato: Vurdering.Avklart<LocalDate, Grunnlagsdata>) {
        val forventetMaksdato = LocalDate.of(2019, 11, 28)

        assert(maksdato.fastsattVerdi).isEqualTo(forventetMaksdato)
        assert(maksdato.begrunnelse).isEqualTo("§ 8-12: ARBEIDSTAKER på 48 år gir maks 248 dager. 10 av disse er forbrukt")
        assert(maksdato.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
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
        assert(medlemsskap.vurderingstidspunkt).isBetween(LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1))
        assert(medlemsskap.fastsattAv).isEqualTo("SPA")

        assert(medlemsskap.grunnlag.bostedland).isEqualTo(landskodeNORGE)
        assert(medlemsskap.grunnlag.statsborgerskap).isEqualTo(landskodeNORGE)
        assert(medlemsskap.grunnlag.status).isEqualTo(bosattstatus)
        assert(medlemsskap.grunnlag.diskresjonskode).isNull()
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

        //checkInntekt(beregningsperiode.inntekter, beregningsgrunnlagStart, beregningsgrunnlagStart.plusMonths(2).with(lastDayOfMonth()))
    }

    private fun checkBeregningsperiode12mnd(beregningsperiode: Beregningsperiode) {
        assert(beregningsperiode.begrunnelse).isNotEmpty()

        //checkInntekt(beregningsperiode.inntekter, sammenligningsgrunnlagStart, sammenligningsgrunnlagStart.plusMonths(11).with(lastDayOfMonth()))
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

    private fun checkArbeidsforholdVurdering(arbeidsforholdVurdering: Vurdering.Avklart<Boolean, List<Arbeidsforhold>>) {
        assert(arbeidsforholdVurdering.fastsattVerdi).isTrue()
        assert(arbeidsforholdVurdering.begrunnelse).isEqualTo("Søker har et aktivt arbeidsforhold hos MATBUTIKKEN")
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

    private fun checkSykepengeliste(sykepengeliste: List<AnvistPeriode>) {
        assert(sykepengeliste).isEqualTo(tiDagerSykepengeHistorikk())
    }

    private fun checkVilkårsprøving(vilkårsprøving: Evaluering) {
        // Detaljene i vilkårsprøving testes ikke her, men resultatet for dette caset skal være JA
        assert(vilkårsprøving.resultat).isEqualTo(Resultat.JA)
    }

    private fun checkBeregning(beregning: Beregningsresultat) {
        // Detaljene i beregningen testes ikke her, men resultatet for dette caset skal være 23 virkedager a 1154,-
        assert(beregning.dagsatser).hasSize(23)
        assert(beregning.dagsatser).each {
            val dagsats = it.actual
            assert(dagsats.dato).isBetween(første_dag_i_syketilfelle, siste_dag_i_syketilfelle)
            assert(dagsats.dato.dayOfWeek).isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            assert(dagsats.sats).isEqualTo(1154L)
            assert(dagsats.skalUtbetales).isTrue()
        }
    }

    private fun checkVedtak(vedtak: Vedtak) {
        assert(vedtak.perioder).containsExactly(Vedtaksperiode(fom = LocalDate.of(2019, 1, 1), tom = LocalDate.of(2019, 1, 31), dagsats = BigDecimal.valueOf(1154L), fordeling = listOf(Fordeling(mottager = "97114455", andel = 100))))
    }

    val første_dag_i_syketilfelle = parse("2019-01-01")
    val siste_dag_i_syketilfelle = parse("2019-01-31")

    private fun produserSykepengesøknadV2(aktørId: String): SykepengesøknadV2DTO {
        val søknad = SykepengesøknadV2DTO(
                id = "1",
                aktorId = aktørId,
                type = SoknadstypeDTO.ARBEIDSTAKERE,
                status = SoknadsstatusDTO.SENDT,
                arbeidsgiver = ArbeidsgiverDTO(navn = "MATBUTIKKEN", orgnummer = stubbet_arbeidsforhold.arbeidsgiver.identifikator),
                fom = første_dag_i_syketilfelle,
                tom = siste_dag_i_syketilfelle,
                startSyketilfelle = første_dag_i_syketilfelle,
                sendtNav = parse("2019-01-17").atStartOfDay(),
                soknadsperioder = listOf(
                        SoknadsperiodeDTO(
                                fom = første_dag_i_syketilfelle,
                                tom = siste_dag_i_syketilfelle,
                                sykmeldingsgrad = 100
                        )
                ),
                soktUtenlandsopphold = false,
                andreInntektskilder = emptyList(),
                fravar = emptyList()
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

    private fun ventPåVedtak(): SykepengeVedtak {
        val resultConsumer = KafkaConsumer<String, SykepengeVedtak>(consumerProperties(), StringDeserializer(), SykepengeVedtakDeserializer())
        resultConsumer.subscribe(listOf(VEDTAK_SYKEPENGER.name))

        val end = System.currentTimeMillis() + 20 * 1000

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

        throw RuntimeException("fant ingen vedtak etter 20 sekunder")
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
        producer.send(ProducerRecord(SYKEPENGESØKNADER_INN.name, message.id, defaultObjectMapper.valueToTree(message)))
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
                .willReturn(okJson(defaultObjectMapper.writeValueAsString(
                        StsRestClient.Token(
                                accessToken = "test token",
                                type = "Bearer",
                                expiresIn = 3600
                        )
                ))))
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

    val stubbet_arbeidsforhold = Arbeidsforhold(
            type = "Arbeidstaker",
            arbeidsgiver = Arbeidsgiver(
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
        stubFor(any(urlPathEqualTo("/api/person/$aktørId"))
                .willReturn(okJson(defaultObjectMapper.writeValueAsString(stubbet_person))))
    }

    private fun inntektStub(aktørId: String) {
        stubFor(any(urlPathEqualTo("/api/inntekt/$aktørId/beregningsgrunnlag/${stubbet_arbeidsforhold.arbeidsgiver.identifikator}"))
                .willReturn(okJson(defaultObjectMapper.writeValueAsString(
                        InntektsoppslagResultat(stubbet_inntekt_beregningsgrunnlag)
                ))))

        stubFor(any(urlPathEqualTo("/api/inntekt/$aktørId/sammenligningsgrunnlag"))
                .willReturn(okJson(defaultObjectMapper.writeValueAsString(
                        InntektsoppslagResultat(stubbet_inntekt_sammenligningsgrunnlag)
                ))))
    }

    private fun sykepengehistorikkStub(aktørId: String) {
        stubFor(any(urlPathEqualTo("/api/sykepengehistorikk/$aktørId"))
                .willReturn(okJson(defaultObjectMapper.writeValueAsString(
                        tiDagerSykepengeHistorikk()
                ))))
    }

    private fun tiDagerSykepengeHistorikk() : List<AnvistPeriode> {
        val someMonday = første_dag_i_syketilfelle.minusMonths(1).with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        return listOf(AnvistPeriode(someMonday, someMonday.plusDays(13)))
    }

    private fun arbeidsforholdStub(aktørId: String) {

        val arbeidsforholdWrapper = ArbeidsforholdWrapper(
                arbeidsforhold = arrayOf(ArbeidsforholdMedInntekter(stubbet_arbeidsforhold))
        )

        stubFor(any(urlPathEqualTo("/api/arbeidsforhold/$aktørId/inntekter"))
                .willReturn((okJson(defaultObjectMapper.writeValueAsString(arbeidsforholdWrapper)))))
    }
}
