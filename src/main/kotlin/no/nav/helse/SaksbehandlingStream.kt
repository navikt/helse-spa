package no.nav.helse

import io.prometheus.client.Counter
import no.nav.helse.streams.*
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

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

    private fun topology(): Topology {
        val builder = StreamsBuilder()
        val stream: KStream<String, JSONObject> = builder.consumeTopic(Topics.SYKEPENGEBEHANDLING)

        stream.peek { _, _ -> acceptCounter.labels("accepted").inc() }
                .mapValues { _, soknad -> somSoknad(soknad) }
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

    /*
        parse with jackson?
     */
    fun somSoknad(input: JSONObject): Sykepengesoknad = Sykepengesoknad(aktorId = "",
            harVurdertInntekt = false,
            andreYtelser = emptyList(),
            fom = LocalDate.now(),
            tom = LocalDate.now(),
            sendtNav = LocalDateTime.now(),
            soknadsperioder = emptyList(),
            soktUtenlandsopphold = false,
            startSyketilfelle = LocalDate.now())

    fun hentRegisterData(input: Sykepengesoknad): BeriketSykepengesoknad =
            BeriketSykepengesoknad(input, Faktagrunnlag(hentTPSData(input)))

    fun fastsettFakta(input: BeriketSykepengesoknad): BeriketSykepengesoknad = input
    fun prøvVilkår(input: BeriketSykepengesoknad): BeriketSykepengesoknad = input
    fun beregnSykepenger(input: BeriketSykepengesoknad): BeriketSykepengesoknad = input
    fun fattVedtak(input: BeriketSykepengesoknad): JSONObject = JSONObject(input.originalSoknad)
}
