package no.nav.helse

import io.prometheus.client.Counter
import no.nav.helse.serde.sykepengesoknadSerde
import no.nav.helse.streams.*
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.json.JSONObject

class SaksbehandlingStream(val env: Environment) {
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

    private fun topology(): Topology {
        val builder = StreamsBuilder()
        val stream: KStream<String, Sykepengesoknad> = builder.consumeTopic(sykepengesoknadTopic)

        stream.peek { _, _ -> acceptCounter.labels("accepted").inc() }
                .mapValues { _, soknad -> hentRegisterData(soknad) }
                .mapValues { _, soknad -> fastsettFakta(soknad) }
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

    fun hentRegisterData(input: Sykepengesoknad): BeriketSykepengesoknad =
            BeriketSykepengesoknad(input, Faktagrunnlag(tps = PersonOppslag(env.sparkelBaseUrl).hentTPSData(input)))

    fun fastsettFakta(input: BeriketSykepengesoknad): AvklartSykepengesoknad = AvklartSykepengesoknad(input.originalSoknad, vurderMedlemskap(input))
    fun prøvVilkår(input: AvklartSykepengesoknad): AvklartSykepengesoknad = input
    fun beregnSykepenger(input: AvklartSykepengesoknad): AvklartSykepengesoknad = input
    fun fattVedtak(input: AvklartSykepengesoknad): JSONObject = JSONObject(input.originalSoknad)
}

val sykepengesoknadTopic = Topic(
        name = Topics.SYKEPENGESØKNADER_INN.name,
        keySerde = Serdes.String(),
        valueSerde = sykepengesoknadSerde
)
