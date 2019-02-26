package no.nav.helse

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import no.nav.NarePrometheus
import no.nav.helse.fastsetting.fastsettingAvSykepengegrunnlagetNårTrygdenYterSykepenger
import no.nav.helse.fastsetting.vurderAlderPåSisteDagISøknadsPeriode
import no.nav.helse.fastsetting.vurderArbeidsforhold
import no.nav.helse.fastsetting.vurderMaksdato
import no.nav.helse.fastsetting.vurderMedlemskap
import no.nav.helse.serde.sykepengesoknadSerde
import no.nav.helse.streams.*
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.apache.kafka.streams.kstream.*
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.*

class SaksbehandlingStream(val env: Environment) {
    private val log = LoggerFactory.getLogger(SaksbehandlingStream::class.java)
    private val stsClient = StsRestClient(baseUrl = env.stsRestUrl, username = env.username, password = env.password)
    private val acceptCounter: Counter = Counter.build()
            .name("spa_behandling_stream_counter")
            .labelNames("state")
            .help("Antall meldinger SaksbehandlingsStream i SPA har godtatt og forsøkt behandlet")
            .register()

    private val appId = "spa-behandling"

    private val consumer: StreamConsumer

    init {
        val streamConfig = if ("true"==env.plainTextKafka) streamConfigPlainTextKafka() else streamConfig(appId, env.bootstrapServersUrl,
                env.username to env.password,
                env.navTruststorePath to env.navTruststorePassword)
        consumer = StreamConsumer(appId, KafkaStreams(topology(), streamConfig))
    }

    private fun streamConfigPlainTextKafka() : Properties = Properties().apply {
        log.warn("Using kafka plain text config only works in development!")
        put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServersUrl)
        put(StreamsConfig.APPLICATION_ID_CONFIG, appId)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler::class.java)
    }


    private fun topology(): Topology {
        val builder = StreamsBuilder()
        val stream: KStream<String, Sykepengesoknad> = builder.consumeTopic(sykepengesoknadTopic)

        stream.peek { _, _ -> acceptCounter.labels("accepted").inc() }
                .mapValues { _, soknad -> hentRegisterData(soknad) }
                .mapValues { _, soknad -> fastsettFakta(soknad) }
                .mapValues { _, soknad -> beregnMaksdato(soknad) }
                .mapValues { _, soknad -> prøvVilkår(soknad) }
                .mapValues { _, soknad -> beregnSykepenger(soknad) }
                .mapValues { _, soknad -> fattVedtak(soknad) }
                .peek { _, _ -> acceptCounter.labels("processed").inc() }
                .toTopic(Topics.VEDTAK_SYKEPENGER)

        return builder.build()
    }

    fun start() {
        consumer.start()
    }

    fun stop() {
        consumer.stop()
    }

    fun hentRegisterData(input: Sykepengesoknad): BeriketSykepengesøknad =
            BeriketSykepengesøknad(input, Faktagrunnlag(
                    tps = PersonOppslag(env.sparkelBaseUrl, stsClient).hentTPSData(input),
                    inntekt = InntektOppslag(env.sparkelBaseUrl, stsClient).hentInntekt(input.aktorId, input.startSyketilfelle, input.startSyketilfelle.minusYears(1)),
                    sykepengeliste = SykepengelisteOppslag(env.sparkelBaseUrl, stsClient).hentSykepengeliste(input.aktorId, input.fom),
                    arbeidsforhold = ArbeidsforholdOppslag(env.sparkelBaseUrl, stsClient).hentArbeidsforhold(input))
            )

    fun fastsettFakta(input: BeriketSykepengesøknad): AvklartSykepengesoknad = AvklartSykepengesoknad(
            originalSoknad = input.originalSoknad,
            medlemskap = vurderMedlemskap(input),
            alder = vurderAlderPåSisteDagISøknadsPeriode(input),
            arbeidsforhold = vurderArbeidsforhold(input),
            sykepengeliste = input.faktagrunnlag.sykepengeliste,
            sykepengegrunnlag = fastsettingAvSykepengegrunnlagetNårTrygdenYterSykepenger(input.originalSoknad.startSyketilfelle, input.originalSoknad.arbeidsgiver, input.faktagrunnlag.inntekt))
    fun beregnMaksdato(soknad: AvklartSykepengesoknad): AvklartSykepengesoknad = soknad.copy(maksdato = vurderMaksdato(soknad))
    fun prøvVilkår(input: AvklartSykepengesoknad): AvklartSykepengesoknad = input
    fun beregnSykepenger(input: AvklartSykepengesoknad): AvklartSykepengesoknad = input
    fun fattVedtak(input: AvklartSykepengesoknad): JSONObject = JSONObject(input)
}

val sykepengesoknadTopic = Topic(
        name = Topics.SYKEPENGESØKNADER_INN.name,
        keySerde = Serdes.String(),
        valueSerde = sykepengesoknadSerde
)

val narePrometheus = NarePrometheus(CollectorRegistry.defaultRegistry)
