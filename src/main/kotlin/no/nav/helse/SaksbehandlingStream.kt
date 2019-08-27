package no.nav.helse

import arrow.core.Either
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behandling.Oppslag
import no.nav.helse.behandling.Sakskompleks
import no.nav.helse.behandling.SykepengeVedtak
import no.nav.helse.oppslag.StsRestClient
import no.nav.helse.probe.SaksbehandlingProbe
import no.nav.helse.serde.JsonNodeSerde
import no.nav.helse.streams.StreamConsumer
import no.nav.helse.streams.Topics.SYKEPENGEBEHANDLINGSFEIL
import no.nav.helse.streams.Topics.VEDTAK_SYKEPENGER
import no.nav.helse.streams.defaultObjectMapper
import no.nav.helse.streams.streamConfig
import no.nav.helse.streams.toTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Predicate
import org.slf4j.MDC
import java.util.Properties

const val SAKSKOMPLEKS_TOPIC = "privat-sakskompleks" //TODO: Hvor skal denne bo?

class SaksbehandlingStream(val env: Environment) {

    private val stsClient = StsRestClient(baseUrl = env.stsRestUrl, username = env.username, password = env.password)

    private val probe = SaksbehandlingProbe(env)
    private val oppslag = Oppslag(env.sparkelBaseUrl, stsClient)

    private val appId = "spa-behandling-1"

    private val consumer: StreamConsumer

    init {
        val streamConfig = if ("true" == env.plainTextKafka) streamConfigPlainTextKafka() else streamConfig(appId, env.bootstrapServersUrl,
                env.kafkaUsername to env.kafkaPassword,
                env.navTruststorePath to env.navTruststorePassword)
        consumer = StreamConsumer(appId, KafkaStreams(topology(oppslag, probe), streamConfig))
    }

    private fun streamConfigPlainTextKafka(): Properties = Properties().apply {
        probe.startKakaWithPlainText()
        put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServersUrl)
        put(StreamsConfig.APPLICATION_ID_CONFIG, appId)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler::class.java)
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun topology(oppslag: Oppslag, probe: SaksbehandlingProbe): Topology {
            val builder = StreamsBuilder()

            val (feilendeSakskompleks,
                vedtak) = builder.stream<String, JsonNode>(SAKSKOMPLEKS_TOPIC, Consumed.with(Serdes.String(), JsonNodeSerde(objectMapper))
                .withOffsetResetPolicy(Topology.AutoOffsetReset.EARLIEST))
                .mapValues { jsonNode -> Sakskompleks(jsonNode) }
                .filter { _, sakskompleks -> sakskompleks.søknader.size == 1 }
                .mapValues { _, sakskompleks ->
                    loggMedSakskompleksId(sakskompleks.id) {
                        sakskompleks.behandle(oppslag, probe)
                    }
                }.branch(
                    Predicate { _, søknad -> søknad is Either.Left },
                    Predicate { _, søknad -> søknad is Either.Right }
                )

            sendTilFeilkø(probe, feilendeSakskompleks)
            sendTilVedtakskø(probe, vedtak)

            return builder.build()
        }

        private fun sendTilVedtakskø(probe: SaksbehandlingProbe, vedtak: KStream<String, Either<Behandlingsfeil, SykepengeVedtak>>) {
            vedtak
                    .peek { _, _ -> probe.behandlingOk() }
                    .mapValues { _, sykepengevedtak -> (sykepengevedtak as Either.Right).b }
                    .peek { _, sykepengevedtak ->
                        loggMedSakskompleksId(sykepengevedtak.sakskompleks.id) {
                            probe.vedtakBehandlet(sykepengevedtak)
                        }
                    }.mapValues { _, sykepengevedtak ->
                        loggMedSakskompleksId(sykepengevedtak.sakskompleks.id) {
                            serialize(sykepengevedtak)
                        }
                    }.toTopic(VEDTAK_SYKEPENGER)
        }

        private fun sendTilFeilkø(probe: SaksbehandlingProbe, feilendeSøknader: KStream<String, Either<Behandlingsfeil, SykepengeVedtak>>) {
            feilendeSøknader
                    .peek { _, _ -> probe.behandlingFeil() }
                    .mapValues { _, behandlingsfeil -> (behandlingsfeil as Either.Left).a }
                    .peek { _, behandlingsfeil ->
                        loggMedSakskompleksId(behandlingsfeil.sakskompleksId) {
                            probe.behandlingsFeilMedType(behandlingsfeil)
                        }
                    }.mapValues { _, behandlingsfeil ->
                        loggMedSakskompleksId(behandlingsfeil.sakskompleksId) {
                            serializeBehandlingsfeil(behandlingsfeil)
                        }
                    }.toTopic(SYKEPENGEBEHANDLINGSFEIL)
        }
    }

    fun start() {
        consumer.start()
    }

    fun stop() {
        consumer.stop()
    }

}

private fun <T> loggMedSakskompleksId(sakskompleksId: String, block: () -> T): T {
    try {
        MDC.put("sakskompleksId", sakskompleksId)
        return block()
    } finally {
        MDC.remove("sakskompleksId")
    }
}

fun serialize(vedtak: SykepengeVedtak): JsonNode = defaultObjectMapper.valueToTree(vedtak)
fun serializeBehandlingsfeil(feil: Behandlingsfeil): JsonNode = defaultObjectMapper.valueToTree(feil)
