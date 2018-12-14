package no.nav.helse

import io.prometheus.client.Counter
import no.nav.helse.streams.*
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("Saksbehandlingsstrøm")

class SaksbehandlingStream(env: Environment) {
    private val acceptCounter: Counter = Counter.build()
            .name("spa_behandling_stream_counter")
            .labelNames("state")
            .help("Antall meldinger SaksbehandlingsStream i SPA har godtatt og forsøkt behandlet")
            .register()

    private val appId = "spa-behandling"

    private val consumer: StreamConsumer

    init {
        val streamConfig = streamConfig(appId, env.bootstrapServersUrl,
                env.username to env.password,
                env.navTruststorePath to env.navTruststorePassword)
        consumer = StreamConsumer(appId, KafkaStreams(topology(), streamConfig))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun canAccept(key: String?, value: JSONObject): Boolean {
        // if we can parse the input _and_ it actually has a søknads-nummer
        return try {
            !JSONToSoknadMapper().apply(value).id.isEmpty()
        } catch(e: Exception){
            if (e is JSONException) log.info("Couldn't parse the message: {}.", e.message)
            acceptCounter.labels("rejected").inc()
            false
        }
    }

    private fun topology(): Topology {
        val builder = StreamsBuilder()
        val stream: KStream<String, JSONObject> = builder.consumeTopic(Topics.SYKEPENGEBEHANDLING)

        stream.filter(this::canAccept)
                .peek { _, _-> acceptCounter.labels("accepted").inc() }
                .mapValues(JSONToSoknadMapper())
                .mapValues { value -> value.evaluer() }
                .mapValues { value -> JSONObject(value) }
                .peek { _, _-> acceptCounter.labels("processed").inc() }
                .toTopic(Topics.VEDTAK_SYKEPENGER)

        return builder.build()
    }

    fun start() {
        consumer.start()
    }
    fun stop() {
        consumer.stop()
    }
}
