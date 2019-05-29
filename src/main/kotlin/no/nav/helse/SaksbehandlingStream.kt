package no.nav.helse

import arrow.core.Either
import com.fasterxml.jackson.databind.JsonNode
import com.github.kittinunf.fuel.core.FuelManager
import io.prometheus.client.Counter
import no.nav.helse.behandling.Oppslag
import no.nav.helse.behandling.SykepengeVedtak
import no.nav.helse.behandling.søknad.Sykepengesøknad
import no.nav.helse.oppslag.StsRestClient
import no.nav.helse.probe.SaksbehandlingProbe
import no.nav.helse.streams.*
import no.nav.helse.streams.Topics.SYKEPENGEBEHANDLINGSFEIL
import no.nav.helse.streams.Topics.VEDTAK_SYKEPENGER
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Predicate
import org.slf4j.MDC
import java.util.*

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
        val requestCounter = Counter.build("http_outgoing_requests_total", "statuskoder for utgående kall")
                .labelNames("code", "method", "protocol", "host")
                .register()

        fun topology(oppslag: Oppslag, probe: SaksbehandlingProbe): Topology {
            val builder = StreamsBuilder()

            val (feilendeSøknader,
                    vedtak) = builder.consumeTopic(Topics.SYKEPENGESØKNADER_INN)
                    .peek { søknadId, _ ->
                        loggMedSøknadId(søknadId) {
                            probe.mottattSøknadUansettStatusOgType(søknadId)
                        }
                    }
                    .mapValues { søknadId, jsonNode ->
                        loggMedSøknadId(søknadId) {
                            Sykepengesøknad(jsonNode)
                        }
                    }
                    .filter { _, søknad ->
                        søknad.status != "UKJENT"
                    }
                    .peek { søknadId, søknad ->
                        loggMedSøknadId(søknadId) {
                            probe.mottattSøknadUansettType(søknad.id, søknad.status)
                        }
                    }
                    .filter { _, søknad ->
                        søknad.type == "OPPHOLD_UTLAND" ||
                                søknad.type == "SELVSTENDIGE_OG_FRILANSERE" ||
                                søknad.type == "ARBEIDSTAKERE"
                    }
                    .peek { søknadId, søknad ->
                        loggMedSøknadId(søknadId) {
                            probe.mottattSøknad(søknad.id, søknad.status, søknad.type)
                        }
                    }
                    .filter { _, søknad ->
                        søknad.sendtTilNAV
                    }
                    .peek { søknadId, søknad ->
                        loggMedSøknadId(søknadId) {
                            probe.mottattSøknadSendtNAV(søknadId, søknad.type)
                        }
                    }
                    .mapValues { _, søknad ->
                        loggMedSøknadId(søknad.id) {
                            søknad.behandle(oppslag, probe)
                        }
                    }.branch(
                            Predicate { _, søknad -> søknad is Either.Left },
                            Predicate { _, søknad -> søknad is Either.Right }
                    )

            sendTilFeilkø(probe, feilendeSøknader)
            sendTilVedtakskø(probe, vedtak)

            return builder.build()
        }

        private fun sendTilVedtakskø(probe: SaksbehandlingProbe, vedtak: KStream<String, Either<Behandlingsfeil, SykepengeVedtak>>) {
            vedtak
                    .peek { _, _ -> probe.behandlingOk() }
                    .mapValues { _, sykepengevedtak -> (sykepengevedtak as Either.Right).b }
                    .peek { _, sykepengevedtak ->
                        loggMedSøknadId(sykepengevedtak.originalSøknad.id) {
                            probe.vedtakBehandlet(sykepengevedtak)
                        }
                    }.mapValues { _, sykepengevedtak ->
                        loggMedSøknadId(sykepengevedtak.originalSøknad.id) {
                            serialize(sykepengevedtak)
                        }
                    }.toTopic(VEDTAK_SYKEPENGER)
        }

        private fun sendTilFeilkø(probe: SaksbehandlingProbe, feilendeSøknader: KStream<String, Either<Behandlingsfeil, SykepengeVedtak>>) {
            feilendeSøknader
                    .peek { _, _ -> probe.behandlingFeil() }
                    .mapValues { _, behandlingsfeil -> (behandlingsfeil as Either.Left).a }
                    .peek { _, behandlingsfeil ->
                        loggMedSøknadId(behandlingsfeil.soknadId) {
                            probe.behandlingsFeilMedType(behandlingsfeil)
                        }
                    }.mapValues { _, behandlingsfeil ->
                        loggMedSøknadId(behandlingsfeil.soknadId) {
                            serializeBehandlingsfeil(behandlingsfeil)
                        }
                    }.toTopic(SYKEPENGEBEHANDLINGSFEIL)
        }
    }

    fun start() {
        FuelManager.instance.addResponseInterceptor { next ->
            { request, response ->
                requestCounter.labels("${response.statusCode}", request.method.value,
                        request.url.protocol, request.url.host).inc()
                next(request, response)
            }
        }

        consumer.start()
    }

    fun stop() {
        consumer.stop()
    }

}

private fun <T> loggMedSøknadId(søknadId: String, block: () -> T): T {
    try {
        MDC.put("soknadId", søknadId)
        return block()
    } finally {
        MDC.remove("soknadId")
    }
}

fun serialize(vedtak: SykepengeVedtak): JsonNode = defaultObjectMapper.valueToTree(vedtak)
fun serializeBehandlingsfeil(feil: Behandlingsfeil): JsonNode = defaultObjectMapper.valueToTree(feil)
