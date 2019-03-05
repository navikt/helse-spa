package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import no.nav.NarePrometheus
import no.nav.helse.behandling.*
import no.nav.helse.fastsetting.vurderFakta
import no.nav.helse.streams.StreamConsumer
import no.nav.helse.streams.consumeTopic
import no.nav.helse.streams.streamConfig
import no.nav.helse.streams.toTopic
import no.nav.helse.oppslag.StsRestClient
import no.nav.helse.streams.*
import no.nav.helse.streams.Topics.SYKEPENGEBEHANDLINGSFEIL
import no.nav.helse.streams.Topics.SYKEPENGESØKNADER_INN
import no.nav.helse.streams.Topics.VEDTAK_SYKEPENGER
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
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
    private val vedtakCounter: Counter = Counter.build()
            .name("spa_vedtak_stream_counter")
            .help("Antall vedtak fattet av SPA")
            .register()
    private val behandlingsfeilCounter: Counter = Counter.build()
            .name("spa_behandlingsfeil_counter")
            .labelNames("steg")
            .help("Antall ganger en søknad er forsøkt behandlet uten at vi kommer til et vedtak")
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

        val streams = builder.consumeTopic(SYKEPENGESØKNADER_INN)
                .peek { _, _ -> acceptCounter.labels("accepted").inc() }
                .filter { _, value -> value.has("status") && value.get("status").asText() == "SENDT" }
                .mapValues { _, jsonNode -> deserializeSykepengesøknad(jsonNode) }
                .mapValues { _, søknad -> søknad.flatMap { hentRegisterData(it) } }
                .mapValues { _, faktagrunnlag -> faktagrunnlag.flatMap { fastsettFakta(it) } }
                .mapValues { _, avklarteFakta -> avklarteFakta.flatMap { prøvVilkår(it) } }
                .mapValues { _, vilkårsprøving -> vilkårsprøving.flatMap { beregnSykepenger(it) } }
                .mapValues { _, sykepengeberegning -> sykepengeberegning.flatMap { fattVedtak(it) } }
                .peek { _, _ -> acceptCounter.labels("processed").inc() }
                .branch(
                        Predicate { _, søknad -> søknad is Either.Left },
                        Predicate { _, søknad -> søknad is Either.Right }
                )

        streams[0]
                .mapValues { _, behandlingsfeil -> (behandlingsfeil as Either.Left).left }
                .peek { _, behandlingsfeil -> logAndCountFail(behandlingsfeil) }
                .mapValues { _, behandlingsfeil -> serializeBehandlingsfeil(behandlingsfeil) }
                .toTopic(SYKEPENGEBEHANDLINGSFEIL) // TODO : should be a generic failure-topic

        streams[1]
                .mapValues { _, vedtak -> (vedtak as Either.Right).right }
                .peek { _, vedtak -> logAndCountVedtak(vedtak) }
                .mapValues { _, vedtak -> serialize(vedtak) }
                .toTopic(VEDTAK_SYKEPENGER)

        return builder.build()
    }

    private fun logAndCountFail(behandlingsfeil: Behandlingsfeil) {
        when(behandlingsfeil) {
            is Behandlingsfeil.Deserialiseringsfeil -> behandlingsfeilCounter.labels("deserialisering").inc()
            is Behandlingsfeil.RegisterFeil -> behandlingsfeilCounter.labels("register").inc()
            is Behandlingsfeil.Avklaringsfeil -> behandlingsfeilCounter.labels("avklaring").inc()
            is Behandlingsfeil.Vilkårsprøvingsfeil -> behandlingsfeilCounter.labels("vilkarsproving").inc()
            is Behandlingsfeil.Beregningsfeil -> behandlingsfeilCounter.labels("beregning").inc()
        }
    }
    private fun logAndCountVedtak(vedtak: SykepengeVedtak) {
        vedtakCounter.inc()
    }

    private fun deserializeSykepengesøknad(soknad: JsonNode): Either<Behandlingsfeil, Sykepengesøknad> =
        try {
            Either.Right(defaultObjectMapper.treeToValue(soknad, Sykepengesøknad::class.java))
        } catch(e: MissingKotlinParameterException) {
            log.error("Failed to deserialize søknad due to missing non-nullable parameter: ${e.parameter.name} of type ${e.parameter.type}")
            Either.Left(Behandlingsfeil.manglendeFeilDeserialiseringsfeil(soknad, e))
        } catch (e: Exception) {
            log.error("Failed to deserialize søknad", e)
            Either.Left(Behandlingsfeil.ukjentDeserialiseringsfeil(soknad, e))
        }

    fun start() {
        consumer.start()
    }

    fun stop() {
        consumer.stop()
    }

    private fun hentRegisterData(søknad: Sykepengesøknad): Either<Behandlingsfeil, FaktagrunnlagResultat> = Oppslag(env.sparkelBaseUrl, stsClient).hentRegisterData(søknad)
    private fun fastsettFakta(fakta: FaktagrunnlagResultat): Either<Behandlingsfeil, AvklarteFakta> = vurderFakta(fakta)
    private fun prøvVilkår(fakta: AvklarteFakta): Either<Behandlingsfeil, Vilkårsprøving> = vilkårsprøving(fakta)
    private fun beregnSykepenger(vilkårsprøving: Vilkårsprøving): Either<Behandlingsfeil, Sykepengeberegning> = sykepengeBeregning(vilkårsprøving)
    private fun fattVedtak(beregning: Sykepengeberegning): Either<Behandlingsfeil, SykepengeVedtak> = vedtak(beregning)
}

fun serialize(vedtak: SykepengeVedtak): JsonNode = defaultObjectMapper.readTree(defaultObjectMapper.writeValueAsString(vedtak))
fun serializeBehandlingsfeil(feil: Behandlingsfeil): JsonNode = defaultObjectMapper.readTree(defaultObjectMapper.writeValueAsString(feil))

val narePrometheus = NarePrometheus(CollectorRegistry.defaultRegistry)
