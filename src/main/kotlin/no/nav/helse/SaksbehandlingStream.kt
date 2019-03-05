package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Counter
import no.nav.NarePrometheus
import no.nav.helse.behandling.*
import no.nav.helse.fastsetting.Vurdering
import no.nav.helse.fastsetting.vurderFakta
import no.nav.helse.oppslag.*
import no.nav.helse.serde.JacksonDeserializer
import no.nav.helse.serde.JacksonSerializer
import no.nav.helse.serde.defaultObjectMapper
import no.nav.helse.serde.jsonNodeSerde
import no.nav.helse.streams.StreamConsumer
import no.nav.helse.streams.Topic
import no.nav.helse.streams.Topics
import no.nav.helse.streams.consumeTopic
import no.nav.helse.streams.streamConfig
import no.nav.helse.streams.toTopic
import no.nav.helse.sykepenger.beregning.beregn
import no.nav.nare.core.evaluations.Resultat
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
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
    private val uavklartCounter: Counter = Counter.build()
            .name("spa_uavklarte_fakta_counter")
            .labelNames("fakta")
            .help("Antall ganger et faktum har gått uavklart")
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
        val behandlingFeilet: Predicate<String, Either<Behandlingsfeil, *>> = Predicate { _, søknad -> søknad is Either.Left }

        val streams = builder.consumeTopic(sykepengesoknadTopic)
                .peek { _, _ -> acceptCounter.labels("accepted").inc() }
                .filter { _, value -> value.has("status") && value.get("status").asText() == "SENDT" }
                .mapValues { _, jsonNode -> deserializeSykepengesøknad(jsonNode) }
                .mapValues { _, søknad -> hentRegisterData(søknad) }
                .mapValues { _, faktagrunnlag -> fastsettFakta(faktagrunnlag) }
                .mapValues { _, avklarteFakta -> prøvVilkår(avklarteFakta) }
                .mapValues { _, vilkårsprøving -> beregnSykepenger(vilkårsprøving) }
                .mapValues { _, sykepengeberegning -> fattVedtak(sykepengeberegning) }
                .peek { _, _ -> acceptCounter.labels("processed").inc() }
                .branch(behandlingFeilet, Predicate { _, _ -> true} )

        streams[0]
                .mapValues { _, behandlingsfeil -> (behandlingsfeil as Either.Left).left }
                .peek { _, behandlingsfeil -> logAndCountFail(behandlingsfeil) }
                .mapValues { _, behandlingsfeil -> serialize(behandlingsfeil) }
                .toTopic(uavklartFaktaTopic) // TODO : should be a generic failure-topic

        streams[1]
                .mapValues { _, vedtak -> (vedtak as Either.Right).right }
                .peek { _, vedtak -> logAndCountVedtak(vedtak) }
                .mapValues { _, vedtak -> serialize(vedtak) }
                .toTopic(sykepengevedtakTopic)

        return builder.build()
    }

    // TODO : implement
    private fun logAndCountFail(behandlingsfeil: Behandlingsfeil) {}
    private fun logAndCountVedtak(vedtak: SykepengeVedtak) {}

    private fun tellUavklarte(uavklarteFakta: UavklarteFakta) {
        uavklarteFakta.uavklarteVerdier.asNamedList()
                .filter { it.second is Vurdering.Uavklart<*,*> }
                .forEach { uavklartCounter.labels(it.first).inc() }
    }

    private fun deserializeSykepengesøknad(soknad: JsonNode): Either<Behandlingsfeil, Sykepengesøknad> =
        try {
            Either.Right(defaultObjectMapper.treeToValue(soknad, Sykepengesøknad::class.java))
        } catch(e: MissingKotlinParameterException) {
            log.error("Failed to deserialize søknad due to missing non-nullable parameter: ${e.parameter.name} of type ${e.parameter.type}")
            Either.Left(Behandlingsfeil.from(soknad, e))
        } catch (e: Exception) {
            log.error("Failed to deserialize søknad", e)
            Either.Left(Behandlingsfeil.from(soknad, e))
        }


    fun start() {
        consumer.start()
    }

    fun stop() {
        consumer.stop()
    }

    fun hentRegisterData(eitherSøknadOrFail: Either<Behandlingsfeil, Sykepengesøknad>): Either<Behandlingsfeil, FaktagrunnlagResultat> =
            eitherSøknadOrFail.flatMap {
                Either.Right(FaktagrunnlagResultat(
                        originalSøknad = it,
                        faktagrunnlag = Faktagrunnlag(
                                tps = PersonOppslag(env.sparkelBaseUrl, stsClient).hentTPSData(it),
                                beregningsperiode = Inntektsoppslag(env.sparkelBaseUrl, stsClient).hentBeregningsgrunnlag(it.aktorId, it.startSyketilfelle.minusMonths(3), it.startSyketilfelle.minusMonths(1)),
                                sammenligningsperiode = Inntektsoppslag(env.sparkelBaseUrl, stsClient).hentSammenligningsgrunnlag(it.aktorId, it.startSyketilfelle.minusYears(1), it.startSyketilfelle.minusMonths(1)),
                                sykepengeliste = emptyList(),
                                arbeidsforhold = ArbeidsforholdOppslag(env.sparkelBaseUrl, stsClient).hentArbeidsforhold(it))
                )
                )
            }


    fun fastsettFakta(eitherFaktaOrFail: Either<Behandlingsfeil, FaktagrunnlagResultat>): Either<Behandlingsfeil, AvklarteFakta> = vurderFakta(eitherFaktaOrFail)

    fun prøvVilkår(eitherAvklarteFakta: Either<Behandlingsfeil, AvklarteFakta>): Either<Behandlingsfeil, Vilkårsprøving> = eitherAvklarteFakta.flatMap { avklarteFakta ->
        val vilkårsprøving = Vilkårsprøving(
                originalSøknad = avklarteFakta.originalSøknad,
                faktagrunnlag = avklarteFakta.faktagrunnlag,
                avklarteVerdier = avklarteFakta.avklarteVerdier,
                vilkårsprøving = gjennomførVilkårsvurdering(avklarteFakta))
        when(vilkårsprøving.vilkårsprøving.resultat) {
            Resultat.JA -> Either.Right(vilkårsprøving)
            else -> Either.Left(Behandlingsfeil.from(vilkårsprøving))
        }
    }

    fun beregnSykepenger(eitherVilkårsprøving: Either<Behandlingsfeil, Vilkårsprøving>): Either<Behandlingsfeil, Sykepengeberegning> = eitherVilkårsprøving.flatMap { vilkårsprøving ->
        try {
            val beregningsresultat= beregn(lagBeregninggrunnlag(vilkårsprøving))
            Either.Right(Sykepengeberegning(
                    originalSøknad = vilkårsprøving.originalSøknad,
                    faktagrunnlag = vilkårsprøving.faktagrunnlag,
                    avklarteVerdier = vilkårsprøving.avklarteVerdier,
                    vilkårsprøving = vilkårsprøving.vilkårsprøving,
                    beregning = beregningsresultat))
        } catch(e: Exception) {
            Either.Left(Behandlingsfeil.from(vilkårsprøving, e))
        }
    }


    fun fattVedtak(eitherBeregning: Either<Behandlingsfeil, Sykepengeberegning>): Either<Behandlingsfeil, SykepengeVedtak> = eitherBeregning.flatMap { beregning ->
        Either.Right(
                SykepengeVedtak(
                        originalSøknad = beregning.originalSøknad,
                        faktagrunnlag = beregning.faktagrunnlag,
                        vilkårsprøving = beregning.vilkårsprøving,
                        avklarteVerdier = beregning.avklarteVerdier,
                        beregning = beregning.beregning,
                        vedtak = Vedtak("Burde antagelig gjøre noe med dette.")
                )
        )
    }

    fun serialize(vedtak: SykepengeVedtak): JsonNode = defaultObjectMapper.readTree(defaultObjectMapper.writeValueAsString(vedtak))
    fun serialize(feil: Behandlingsfeil): JsonNode = defaultObjectMapper.readTree(defaultObjectMapper.writeValueAsString(feil))
}

val sykepengesoknadTopic = Topic(
        name = Topics.SYKEPENGESØKNADER_INN.name,
        keySerde = Serdes.String(),
        valueSerde = jsonNodeSerde
)

val sykepengevedtakTopic = Topic(
        name = Topics.VEDTAK_SYKEPENGER.name,
        keySerde = Serdes.String(),
        valueSerde = jsonNodeSerde
)

val uavklartFaktaTopic= Topic(
        name = "privat-helse-sykepenger-uavklart",
        keySerde = Serdes.String(),
        valueSerde = jsonNodeSerde
)

val narePrometheus = NarePrometheus(CollectorRegistry.defaultRegistry)
