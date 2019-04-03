package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.prometheus.client.CollectorRegistry
import no.nav.NarePrometheus
import no.nav.helse.behandling.*
import no.nav.helse.fastsetting.vurderFakta
import no.nav.helse.oppslag.StsRestClient
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
import java.util.*

class SaksbehandlingStream(val env: Environment) {

    private val stsClient = StsRestClient(baseUrl = env.stsRestUrl, username = env.username, password = env.password)
    private val probe = SaksbehandlingProbe(env)


    private val appId = "spa-behandling-1"

    private val consumer: StreamConsumer

    init {
        val streamConfig = if ("true" == env.plainTextKafka) streamConfigPlainTextKafka() else streamConfig(appId, env.bootstrapServersUrl,
                env.kafkaUsername to env.kafkaPassword,
                env.navTruststorePath to env.navTruststorePassword)
        consumer = StreamConsumer(appId, KafkaStreams(topology(), streamConfig))
    }

    private fun streamConfigPlainTextKafka(): Properties = Properties().apply {
        probe.startKakaWithPlainText()
        put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServersUrl)
        put(StreamsConfig.APPLICATION_ID_CONFIG, appId)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler::class.java)
    }

    private fun topology(): Topology {
        val builder = StreamsBuilder()

        val (arbeidstakersøknader, frilanssøknader, alleAndreSøknader) = splittPåType(builder)

        frilanssøknader.peek { _, value -> probe.mottattFrilansSøknad(value) }
        alleAndreSøknader.peek { _, value -> probe.mottattAnnenSøknad(value) }

        val (feilendeSøknader, vedtak) = prøvArbeidstaker(arbeidstakersøknader)

        sendTilFeilkø(feilendeSøknader)
        sendTilVedtakskø(vedtak)

        return builder.build()
    }

    private fun sendTilVedtakskø(vedtak: KStream<String, Either<Behandlingsfeil, SykepengeVedtak>>) {
        vedtak
                .peek { _, _ -> probe.behandlingOk() }
                .mapValues { _, sykepengevedtak -> (sykepengevedtak as Either.Right).right }
                .peek { _, sykepengevedtak -> probe.vedtakBehandlet(sykepengevedtak) }
                .mapValues { _, sykepengevedtak -> serialize(sykepengevedtak) }
                .toTopic(VEDTAK_SYKEPENGER)
    }

    private fun sendTilFeilkø(feilendeSøknader: KStream<String, Either<Behandlingsfeil, SykepengeVedtak>>) {
        feilendeSøknader
                .peek { _, _ -> probe.behandlingFeil() }
                .mapValues { _, behandlingsfeil -> (behandlingsfeil as Either.Left).left }
                .peek { _, behandlingsfeil -> probe.behandlingsFeilMedType(behandlingsfeil) }
                .mapValues { _, behandlingsfeil -> serializeBehandlingsfeil(behandlingsfeil) }
                .toTopic(SYKEPENGEBEHANDLINGSFEIL)
    }

    private fun prøvArbeidstaker(arbeidstakersøknader: KStream<String, JsonNode>): VedtakEllerFeil {
        val (feil, vedtak) = arbeidstakersøknader
                .peek { _, value -> probe.mottattArbeidstakerSøknad(value) }
                .filter { _, value -> value.get("status").asText() == "SENDT" && value.has("sendtNav") && !value.get("sendtNav").isNull }
                .peek { _, value -> probe.mottattSøknadSendtNAV(value) }
                .mapValues { soknadId, jsonNode -> deserializeSykepengesøknadV2(soknadId, jsonNode) }
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
        return VedtakEllerFeil(feil, vedtak)
    }

    private fun splittPåType(builder: StreamsBuilder): SplitByType {
        val (arbeidstakersøknader, frilanssøknader, alleAndreSøknader) = builder.consumeTopic(Topics.SYKEPENGESØKNADER_INN)
                .filter { _, value -> value.has("status") }
                .branch(
                        Predicate { _, value -> value.has("type") },
                        Predicate { _, value -> value.has("soknadstype") },
                        Predicate { _, _ -> true }
                )
        return SplitByType(arbeidstakersøknader = arbeidstakersøknader, frilanssøknader = frilanssøknader, alleAndreSøknader = alleAndreSøknader)
    }


    private fun deserializeSykepengesøknadV2(soknadId: String, soknad: JsonNode): Either<Behandlingsfeil, SykepengesøknadV2DTO> =
            try {
                Either.Right(defaultObjectMapper.treeToValue(soknad, SykepengesøknadV2DTO::class.java))
            } catch (e: MissingKotlinParameterException) {
                probe.missingNonNullablefield(e)
                Either.Left(Behandlingsfeil.manglendeFeilDeserialiseringsfeil(soknadId, soknad, e))
            } catch (e: Exception) {
                probe.failedToDeserialize(e)
                Either.Left(Behandlingsfeil.ukjentDeserialiseringsfeil(soknadId, soknad, e))
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

private data class SplitByType(
        val arbeidstakersøknader: KStream<String, JsonNode>,
        val frilanssøknader: KStream<String, JsonNode>,
        val alleAndreSøknader: KStream<String, JsonNode>
)

private data class VedtakEllerFeil(
        val behandlingsFeil: KStream<String, Either<Behandlingsfeil, SykepengeVedtak>>,
        val vedtak: KStream<String, Either<Behandlingsfeil, SykepengeVedtak>>
)
