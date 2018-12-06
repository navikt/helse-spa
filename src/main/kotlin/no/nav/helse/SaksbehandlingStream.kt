package no.nav.helse

import io.prometheus.client.Counter
import no.nav.helse.streams.*
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.json.JSONObject
import java.time.LocalDate

class SaksbehandlingStream(val env: Environment) {
    private val acceptCounter: Counter = Counter.build()
            .name("spa_behandling_stream_counter")
            .labelNames("state")
            .help("Antall meldinger SaksbehandlingsStream i SPA har godtatt og forsøkt behandlet")
            .register()

    private val groupId = "spa-behandling"

    private val consumer: StreamConsumer

    init {
        val streamConfig = streamConfig(groupId, env.bootstrapServersUrl,
                env.username to env.password,
                env.navTruststorePath to env.navTruststorePassword)
        consumer = StreamConsumer(groupId, KafkaStreams(topology(), streamConfig))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun canAccept(key: String?, value: JSONObject): Boolean {
        return false
    }

    private fun topology(): Topology {
        val builder = StreamsBuilder()
        val stream: KStream<String, JSONObject> = builder.consumeTopic(Topics.SYKEPENGEBEHANDLING)

        stream.filter(this::canAccept)
                .peek { _, _-> acceptCounter.labels("accepted").inc() }
                .mapValues(JSONToSoknadMapper())
                .mapValues { value -> value.evaluer() }
                .mapValues { value -> JSONObject(value) }
                .toTopic(Topics.VEDTAK_SYKEPENGER)

        return builder.build()
    }
}

class JSONToSoknadMapper : ValueMapper<JSONObject, Soknad> {
    override fun apply(value: JSONObject?): Soknad {
        return Soknad("1",
                "foo",
                "00000000000",
                "bar",
                "baz",
                Sykemelding(0.0f, LocalDate.now(), LocalDate.now()),
                emptyList(),
                emptyList(),
                0,
                false,
                emptyList(),
                emptyList(),
                emptyList())
    }
}
