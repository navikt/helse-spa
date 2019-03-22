package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import no.nav.NarePrometheus
import no.nav.helse.Behandlingsfeil.Avklaringsfeil
import no.nav.helse.Behandlingsfeil.Beregningsfeil
import no.nav.helse.Behandlingsfeil.Deserialiseringsfeil
import no.nav.helse.Behandlingsfeil.RegisterFeil
import no.nav.helse.Behandlingsfeil.Vilkårsprøvingsfeil
import no.nav.helse.behandling.AvklarteFakta
import no.nav.helse.behandling.FaktagrunnlagResultat
import no.nav.helse.behandling.Oppslag
import no.nav.helse.behandling.SykepengeVedtak
import no.nav.helse.behandling.Sykepengeberegning
import no.nav.helse.behandling.Sykepengesøknad
import no.nav.helse.behandling.SykepengesøknadV1DTO
import no.nav.helse.behandling.SykepengesøknadV2DTO
import no.nav.helse.behandling.Vilkårsprøving
import no.nav.helse.behandling.asNewPeriode
import no.nav.helse.behandling.mapToSykepengesøknad
import no.nav.helse.behandling.sykepengeBeregning
import no.nav.helse.behandling.vedtak
import no.nav.helse.behandling.vilkårsprøving
import no.nav.helse.domain.Arbeidsgiver
import no.nav.helse.fastsetting.vurderFakta
import no.nav.helse.oppslag.StsRestClient
import no.nav.helse.sensu.InfluxMetricReporter
import no.nav.helse.sensu.SensuClient
import no.nav.helse.streams.StreamConsumer
import no.nav.helse.streams.Topics
import no.nav.helse.streams.Topics.SYKEPENGEBEHANDLINGSFEIL
import no.nav.helse.streams.Topics.VEDTAK_SYKEPENGER
import no.nav.helse.streams.consumeTopic
import no.nav.helse.streams.defaultObjectMapper
import no.nav.helse.streams.streamConfig
import no.nav.helse.streams.toTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.apache.kafka.streams.kstream.Predicate
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

class SaksbehandlingStream(val env: Environment) {

    private val stsClient = StsRestClient(baseUrl = env.stsRestUrl, username = env.username, password = env.password)

    companion object {
        private val log = LoggerFactory.getLogger(SaksbehandlingStream::class.java)

        private val mottattCounter = Counter.build()
                .name("soknader_mottatt_total")
                .labelNames("status", "version")
                .help("Antall søknader mottatt fordelt på status og versjon (v1/v2)")
                .register()
        private val behandlingsCounter = Counter.build()
                .name("soknader_behandlet_total")
                .labelNames("outcome")
                .help("Antall søknader behandlet, fordelt på utfall (ok/feil)")
                .register()
        private val behandlingsfeilCounter = Counter.build()
                .name("behandlingsfeil_total")
                .labelNames("steg")
                .help("Antall ganger en søknad er forsøkt behandlet uten at vi kommer til et vedtak")
                .register()
        private val avklaringsfeilCounter = Counter.build()
                .name("avklaringsfeil_total")
                .labelNames("faktum")
                .help("Hvilke faktum klarer vi ikke fastsette")
                .register()
    }

    private val appId = "spa-behandling-1"

    private val consumer: StreamConsumer

    private val sensuClient = SensuClient(env.sensuHostname, env.sensuPort)
    private val influxMetricReporter = InfluxMetricReporter(sensuClient, "spa-events", mapOf(
            "application" to (System.getenv("NAIS_APP_NAME") ?: "spa"),
            "cluster" to (System.getenv("NAIS_CLUSTER_NAME") ?: "dev-fss"),
            "namespace" to (System.getenv("NAIS_NAMESPACE") ?: "default")
    ))

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

        val streams = builder.consumeTopic(Topics.SYKEPENGESØKNADER_INN)
                .filter { _, value -> value.has("status")}
                .peek { _, value -> mottattCounter.labels(value.get("status").asText(), "v2").inc() }
                .filter { _, value -> value.get("status").asText() == "SENDT" && !value.get("sendtNav").isNull }
                .peek { _, _ -> mottattCounter.labels("SENDT_NAV", "v2").inc() }
                .mapValues { _, jsonNode -> deserializeSykepengesøknadV2(jsonNode) }
                .mapValues { either -> either.flatMap(::mapToSykepengesøknad) }
                .mapValues { _, søknad -> søknad.flatMap { hentRegisterData(it) } }
                .mapValues { _, faktagrunnlag -> faktagrunnlag.flatMap { fastsettFakta(it) } }
                .mapValues { _, avklarteFakta -> avklarteFakta.flatMap { prøvVilkår(it) } }
                .mapValues { _, vilkårsprøving -> vilkårsprøving.flatMap { beregnSykepenger(it) } }
                .mapValues { _, sykepengeberegning -> sykepengeberegning.flatMap { fattVedtak(it) } }
                .branch(
                        Predicate { _, søknad -> søknad is Either.Left },
                        Predicate { _, søknad -> søknad is Either.Right }
                )

        streams[0]
                .peek { _, _ -> behandlingsCounter.labels("feil").inc() }
                .mapValues { _, behandlingsfeil -> (behandlingsfeil as Either.Left).left }
                .peek { _, behandlingsfeil -> logAndCountFail(behandlingsfeil) }
                .mapValues { _, behandlingsfeil -> serializeBehandlingsfeil(behandlingsfeil) }
                .toTopic(SYKEPENGEBEHANDLINGSFEIL)

        streams[1]
                .peek { _, _ -> behandlingsCounter.labels("ok").inc() }
                .mapValues { _, vedtak -> (vedtak as Either.Right).right }
                .peek { _, vedtak -> logAndCountVedtak(vedtak) }
                .mapValues { _, vedtak -> serialize(vedtak) }
                .toTopic(VEDTAK_SYKEPENGER)

        return builder.build()
    }

    private fun logAndCountFail(behandlingsfeil: Behandlingsfeil) {
        log.info(behandlingsfeil.feilmelding)
        when(behandlingsfeil) {
            is Deserialiseringsfeil -> {
                behandlingsfeilCounter.labels("deserialisering").inc()
                influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf("steg" to "deserialisering"))
            }
            is RegisterFeil -> behandlingsfeilCounter.labels("register").inc()
            is Avklaringsfeil -> {
                behandlingsfeilCounter.labels("avklaring").inc()
                behandlingsfeil.tellUavklarte(avklaringsfeilCounter)
                influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf("steg" to "avklaring", "aktorId" to behandlingsfeil.uavklarteFakta.originalSøknad.aktorId))
                log.info("Søknad for aktør ${behandlingsfeil.uavklarteFakta.originalSøknad.aktorId} er uavklart")
            }
            is Vilkårsprøvingsfeil -> {
                behandlingsfeilCounter.labels("vilkarsproving").inc()
                influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf("steg" to "vilkarsproving", "aktorId" to behandlingsfeil.vilkårsprøving.originalSøknad.aktorId))
                log.info("Søknad for aktør ${behandlingsfeil.vilkårsprøving.originalSøknad.aktorId} oppfyller ikke vilkårene")
            }
            is Beregningsfeil -> {
                behandlingsfeilCounter.labels("beregning").inc()
                influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf("steg" to "beregning", "aktorId" to behandlingsfeil.vilkårsprøving.originalSøknad.aktorId))
            }
        }
    }

    private fun logAndCountVedtak(vedtak: SykepengeVedtak) {
        influxMetricReporter.sendDataPoint("vedtak.event", mapOf("aktorId" to vedtak.originalSøknad.aktorId))
        log.info("Søknad for aktør ${vedtak.originalSøknad.aktorId} behandlet OK.")
    }

    private fun deserializeSykepengesøknadV2(soknad: JsonNode): Either<Behandlingsfeil, SykepengesøknadV2DTO> =
        try {
            Either.Right(defaultObjectMapper.treeToValue(soknad, SykepengesøknadV2DTO::class.java))
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

fun serialize(vedtak: SykepengeVedtak): JsonNode = defaultObjectMapper.valueToTree(vedtak)
fun serializeBehandlingsfeil(feil: Behandlingsfeil): JsonNode = defaultObjectMapper.valueToTree(feil)

val narePrometheus = NarePrometheus(CollectorRegistry.defaultRegistry)

// while we do that thing with old topic
fun deserializeSykepengesøknadV1(soknad: JsonNode): Either<Behandlingsfeil, SykepengesøknadV1DTO> =
        try {
            Either.Right(defaultObjectMapper.treeToValue(soknad, SykepengesøknadV1DTO::class.java))
        } catch (e: MissingKotlinParameterException) {
            Either.Left(Behandlingsfeil.manglendeFeilDeserialiseringsfeil(soknad, e))
        } catch (e: Exception) {
            Either.Left(Behandlingsfeil.ukjentDeserialiseringsfeil(soknad, e))
        }

fun mapSykepengesøknadV1ToSykepengesøknadV2(legacy: SykepengesøknadV1DTO): SykepengesøknadV2DTO =
        SykepengesøknadV2DTO(
                    aktorId = legacy.aktorId,
                    arbeidsgiver = Arbeidsgiver(navn = legacy.arbeidsgiver?: "TOM ASA", orgnummer = "00000000000"),
                    fom = legacy.fom ?: YearMonth.now().atDay(1),
                    tom = legacy.tom ?: YearMonth.now().atEndOfMonth(),
                    startSyketilfelle = legacy.startSykeforlop?: LocalDate.now(),
                    soktUtenlandsopphold = false,
                    soknadsperioder = legacy.soknadPerioder.map { asNewPeriode(it) },
                    sendtNav = legacy.innsendtDato?.atStartOfDay(),
                    status = legacy.status
            )
