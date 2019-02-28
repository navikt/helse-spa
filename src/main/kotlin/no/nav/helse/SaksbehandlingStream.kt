package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import no.nav.NarePrometheus
import no.nav.helse.fastsetting.vurderFakta
import no.nav.helse.serde.defaultObjectMapper
import no.nav.helse.serde.jsonNodeSerde
import no.nav.helse.streams.StreamConsumer
import no.nav.helse.streams.Topic
import no.nav.helse.streams.Topics
import no.nav.helse.streams.consumeTopic
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
        val stream = builder.consumeTopic(sykepengesoknadTopic)
                .peek { _, _ -> acceptCounter.labels("accepted").inc() }
                .mapValues { _, jsonNode -> deserializeSykepengesøknad(jsonNode) }
                .filter { _, søknad -> søknad.isPresent }
                .mapValues { _, søknad -> søknad.get() }

        val alleVerdierErAvklart: Predicate<String, AvklaringsResultat> = Predicate { _, søknad -> søknad is AvklarteFakta }

        val avklarteEllerUavklarte: Array<out KStream<String, AvklaringsResultat>> = stream
                        .mapValues { _, søknad -> hentRegisterData(søknad) }
                        .mapValues { _, faktagrunnlag -> fastsettFakta(faktagrunnlag) }
                        .branch(alleVerdierErAvklart)
        //avklarteEllerUavklarte[1].to(Topics.UAVKLARTE_SØKNADER)

        avklarteEllerUavklarte[0].mapValues { _, avklarteFakta -> prøvVilkår(avklarteFakta as AvklarteFakta) }
                .mapValues { _, vilkårsprøving -> beregnSykepenger(vilkårsprøving) }
                .mapValues { _, sykepengeberegning -> fattVedtak(sykepengeberegning) }
                .peek { _, _ ->
                    acceptCounter.labels("processed").inc()
                    log.error("processing message 6")
                }
                .toTopic(sykepengevedtakTopic)

        return builder.build()
    }


    private fun deserializeSykepengesøknad(soknad: JsonNode?): Optional<Sykepengesøknad> =
        try {
            Optional.of(defaultObjectMapper.treeToValue(soknad, Sykepengesøknad::class.java))
        } catch (e: Exception) {
            log.error("Failed to deserialize søknad", e)
            Optional.empty()
        }


    fun start() {
        consumer.start()
    }

    fun stop() {
        consumer.stop()
    }

    fun hentRegisterData(søknad: Sykepengesøknad): FaktagrunnlagResultat =
            FaktagrunnlagResultat(originalSøknad = søknad,
                    faktagrunnlag = Faktagrunnlag(
                            tps = PersonOppslag(env.sparkelBaseUrl, stsClient).hentTPSData(søknad),
                            beregningsperiode = Inntektsoppslag(env.sparkelBaseUrl, stsClient).hentBeregningsgrunnlag(søknad.aktorId, søknad.startSyketilfelle, søknad.startSyketilfelle.minusMonths(3)),
                            sammenligningsperiode = Inntektsoppslag(env.sparkelBaseUrl, stsClient).hentSammenligningsgrunnlag(søknad.aktorId, søknad.startSyketilfelle, søknad.startSyketilfelle.minusYears(1)),
                            sykepengeliste = emptyList(),
                            arbeidsforhold = ArbeidsforholdOppslag(env.sparkelBaseUrl, stsClient).hentArbeidsforhold(søknad))
            )

    fun fastsettFakta(fakta: FaktagrunnlagResultat): AvklaringsResultat = vurderFakta(fakta)

    fun prøvVilkår(avklarteFakta: AvklarteFakta): Vilkårsprøving = Vilkårsprøving(
            originalSøknad = avklarteFakta.originalSøknad,
            faktagrunnlag = avklarteFakta.faktagrunnlag,
            avklarteVerdier = avklarteFakta.avklarteVerdier,
            vilkårsprøving = gjennomførVilkårsvurdering(avklarteFakta))

    fun beregnSykepenger(vilkårsprøving: Vilkårsprøving): Sykepengeberegning =
            Sykepengeberegning(
                    originalSøknad = vilkårsprøving.originalSøknad,
                    faktagrunnlag = vilkårsprøving.faktagrunnlag,
                    avklarteVerdier = vilkårsprøving.avklarteVerdier,
                    vilkårsprøving = vilkårsprøving.vilkårsprøving,
                    beregning = beregn(lagBeregninggrunnlag(vilkårsprøving)))

    fun fattVedtak(bergegning: Sykepengeberegning): JsonNode = defaultObjectMapper.readTree(defaultObjectMapper.writeValueAsString(bergegning))
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
