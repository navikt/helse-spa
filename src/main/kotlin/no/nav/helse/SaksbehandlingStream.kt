package no.nav.helse

import io.prometheus.client.Counter
import no.nav.helse.streams.*
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SaksbehandlingStream(env: Environment) {
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
        // if we can parse the input _and_ it actually has a søknads-nummer
        return try {
            !JSONToSoknadMapper().apply(value).søknadsNr.isEmpty()
        } catch(e: Exception){
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

class JSONToSoknadMapper : ValueMapper<JSONObject, Soknad> {
    override fun apply(value: JSONObject): Soknad {
        return Soknad(value.getString("søknadsNr"),
                value.getString("bruker"),
                value.getString("norskIdent"),
                value.getString("arbeidsgiver"),
                value.getString("sykemeldingId"),
                JSONToSykemeldingMapper().apply(value.getJSONObject("sykemelding")),
                emptyList(),
                emptyList(),
                value.getInt("utdanningsgrad"),
                value.getBoolean("søktOmUtenlandsopphold"),
                emptyList(),
                emptyList(),
                emptyList())
    }
}

class JSONToSykemeldingMapper {
    fun apply(value: JSONObject?): Sykemelding {
        return if (value == null) Sykemelding(0.0f, LocalDate.now(), LocalDate.now())
        else Sykemelding(value.getFloat("grad"),
                value.getLocalDate("fom", "yyyy-MM-dd"),
                value.getLocalDate("tom", "yyyy-MM-dd"))
    }
}

fun JSONObject.getLocalDate(key: String, format: String): LocalDate {
    val rawValue = getString(key)
    return LocalDate.parse(rawValue, DateTimeFormatter.ofPattern(format))
}