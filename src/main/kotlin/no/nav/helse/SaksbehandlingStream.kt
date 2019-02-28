package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import no.nav.NarePrometheus
import no.nav.helse.fastsetting.*
import no.nav.helse.serde.defaultObjectMapper
import no.nav.helse.serde.jsonNodeSerde
import no.nav.helse.streams.StreamConsumer
import no.nav.helse.streams.*
import no.nav.helse.streams.streamConfig
import no.nav.helse.streams.toTopic
import no.nav.helse.sykepenger.beregning.beregn
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Predicate
import org.slf4j.LoggerFactory
import java.time.LocalDate
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
        val streamConfig = if ("true" == env.plainTextKafka) streamConfigPlainTextKafka() else streamConfig(appId, env.bootstrapServersUrl,
                env.kafkaUsername to env.kafkaPassword,
                env.navTruststorePath to env.navTruststorePassword)
        consumer = StreamConsumer(appId, KafkaStreams(topology(), streamConfig))
    }

    private fun streamConfigPlainTextKafka(): Properties = Properties().apply {
        log.warn("Using kafka plain text config only works in development!")
        put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServersUrl)
        put(StreamsConfig.APPLICATION_ID_CONFIG, appId)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler::class.java)
    }

    private fun topology(): Topology {
        val builder = StreamsBuilder()
        val stream: KStream<String, JsonNode> = builder.consumeTopic(sykepengesoknadTopic)

        val alleVerdierErAvklart: Predicate<String, AvklaringsResultat> = Predicate { _, søknad -> søknad is AvklartFakta }

        val avklarteEllerUavklarte: Array<out KStream<String, AvklaringsResultat>> =
                stream.peek { _, _ -> acceptCounter.labels("accepted").inc() }
                        .mapValues { _, soknad -> toSykepengeSoknadOrAny(soknad) }
                        .filter { _, soknad -> soknad is Sykepengesoknad }
                        .mapValues { _, soknad -> soknad as Sykepengesoknad }
                        .mapValues { _, soknad -> hentRegisterData(soknad) }
                        .mapValues { _, soknad -> fastsettFakta(soknad) }
                        .branch(alleVerdierErAvklart)
        //avklarteEllerUavklarte[1].to(Topics.UAVKLARTE_SØKNADER)

        avklarteEllerUavklarte[0].mapValues { _, soknad -> prøvVilkår(soknad as AvklartFakta) }
                .mapValues { _, soknad -> beregnSykepenger(soknad) }
                .mapValues { _, soknad -> fattVedtak(soknad) }
                .peek { _, _ ->
                    acceptCounter.labels("processed").inc()
                    log.error("processing message 6")
                }
                .toTopic(sykepengevedtakTopic)

        return builder.build()
    }


    private fun toSykepengeSoknadOrAny(soknad: JsonNode?): Any {
        try {
            return defaultObjectMapper.treeToValue(soknad, Sykepengesoknad::class.java)
        } catch (e: MissingKotlinParameterException) {
            log.error("Søknaden missing required field/value", e)
            return Any()
        }
    }


    fun start() {
        consumer.start()
    }

    fun stop() {
        consumer.stop()
    }

    fun hentRegisterData(input: Sykepengesoknad): FaktagrunnlagResultat =
            FaktagrunnlagResultat(originalSøknad = input,
                    faktagrunnlag = Faktagrunnlag(
                            tps = PersonOppslag(env.sparkelBaseUrl, stsClient).hentTPSData(input),
                            beregningsperiode = Inntektsoppslag(env.sparkelBaseUrl, stsClient).hentBeregningsgrunnlag(input.aktorId, input.startSyketilfelle, input.startSyketilfelle.minusMonths(3)),
                            sammenligningsperiode = Inntektsoppslag(env.sparkelBaseUrl, stsClient).hentSammenligningsgrunnlag(input.aktorId, input.startSyketilfelle, input.startSyketilfelle.minusYears(1)),
                            sykepengeliste = emptyList(),
                            arbeidsforhold = ArbeidsforholdOppslag(env.sparkelBaseUrl, stsClient).hentArbeidsforhold(input))
            )

    fun fastsettFakta(input: FaktagrunnlagResultat): AvklaringsResultat = vurderFakta(input)

    fun prøvVilkår(input: AvklartFakta): Vilkårsprøving = Vilkårsprøving(
            originalSøknad = input.originalSøknad,
            faktagrunnlag = input.faktagrunnlag,
            avklarteVerdier = input.avklarteVerdier,
            vilkårsprøving = gjennomførVilkårsvurdering(input))

    fun beregnSykepenger(input: Vilkårsprøving): Sykepengeberegning =
            Sykepengeberegning(
                    originalSøknad = input.originalSøknad,
                    faktagrunnlag = input.faktagrunnlag,
                    avklarteVerdier = input.avklarteVerdier,
                    vilkårsprøving = input.vilkårsprøving,
                    beregning = beregn(lagBeregninggrunnlag(input)))

    fun fattVedtak(input: Sykepengeberegning): JsonNode = defaultObjectMapper.readTree(defaultObjectMapper.writeValueAsString(input))
}

val sykepengesoknadTopic = Topic(
        name = Topics.SYKEPENGESØKNADER_INN.name,
        keySerde = Serdes.String(),
        valueSerde = jsonNodeSerde
)

val sykepengevedtakTopic = Topic(
        name = Topics.VEDTAK_SYKEPENGER.name,
        keySerde = Serdes.String(),
        valueSerde = jsonNodeSerde
)

val narePrometheus = NarePrometheus(CollectorRegistry.defaultRegistry)
