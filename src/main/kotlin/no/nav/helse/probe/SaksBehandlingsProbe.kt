package no.nav.helse.probe

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.prometheus.client.Counter
import no.nav.helse.Behandlingsfeil
import no.nav.helse.Environment
import no.nav.helse.SaksbehandlingStream
import no.nav.helse.behandling.SykepengeVedtak
import no.nav.helse.fastsetting.Vurdering
import no.nav.nare.core.evaluations.Evaluering
import org.slf4j.LoggerFactory

class SaksbehandlingProbe(val env: Environment) {
    private val sensuClient = SensuClient(env.sensuHostname, env.sensuPort)

    private val influxMetricReporter = InfluxMetricReporter(sensuClient, "spa-events", mapOf(
            "application" to (System.getenv("NAIS_APP_NAME") ?: "spa"),
            "cluster" to (System.getenv("NAIS_CLUSTER_NAME") ?: "dev-fss"),
            "namespace" to (System.getenv("NAIS_NAMESPACE") ?: "default")
    ))

    companion object {

        private val log = LoggerFactory.getLogger(SaksbehandlingStream::class.java)

        private val mottattCounter = Counter.build()
                .name("soknader_mottatt_total")
                .labelNames("status", "type", "version")
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

    fun startKakaWithPlainText() = log.warn("Using kafka plain text config only works in development!")
    fun missingNonNullablefield(e: MissingKotlinParameterException) = log.error("Failed to deserialize søknad due to missing non-nullable parameter: ${e.parameter.name} of type ${e.parameter.type}")
    fun failedToDeserialize(e: Exception) = log.error("Failed to deserialize søknad", e)

    fun mottattFrilansSøknad(value: JsonNode) = mottattCounter.labels(value.get("status").asText(), value.get("soknadstype").asText(), "v2").inc()
    fun mottattAnnenSøknad(value: JsonNode) = mottattCounter.labels(value.get("status").asText(), value.get("UKJENT").asText(), "v2").inc()
    fun mottattArbeidstakerSøknad(value: JsonNode) = mottattCounter.labels(value.get("status").asText(), value.get("type").asText(), "v2").inc()
    fun mottattSøknadSendtNAV(value: JsonNode) = mottattCounter.labels("SENDT_NAV", value.get("type").asText(), "v2").inc()

    fun gjennomførtVilkårsprøving(value: Evaluering) {

            influxMetricReporter.sendDataPoints(toDatapoints(value))
    }


    fun behandlingsFeilMedType(behandlingsfeil: Behandlingsfeil) {
        log.warn(behandlingsfeil.feilmelding)
        when (behandlingsfeil) {
            is Behandlingsfeil.Deserialiseringsfeil -> serialiseringsFeil(behandlingsfeil)
            is Behandlingsfeil.RegisterFeil -> registerFeil(behandlingsfeil)
            is Behandlingsfeil.Avklaringsfeil -> avklaringsFeil(behandlingsfeil)
            is Behandlingsfeil.Vilkårsprøvingsfeil -> vilkårsPrøvingsFeil(behandlingsfeil)
            is Behandlingsfeil.Beregningsfeil -> beregningsfeil(behandlingsfeil)
        }
    }

    fun serialiseringsFeil(feil: Behandlingsfeil.Deserialiseringsfeil) {
        behandlingsfeilCounter.labels("deserialisering").inc()
        influxMetricReporter.sendDataPoint("behandlingsfeil.event",
                mapOf(
                        "soknadId" to feil.soknadId,
                        "feilmelding" to feil.feilmelding),
                mapOf(
                        "steg" to "deserialisering",
                        "type" to feil.json.get("type").asText()
                ))
    }

    fun registerFeil(feil: Behandlingsfeil.RegisterFeil) {
        behandlingsfeilCounter.labels("register").inc()
        influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf(
                "soknadId" to feil.søknad.id
        ), mapOf(
                "steg" to "register",
                "type" to feil.søknad.type
        ))
    }

    fun avklaringsFeil(feil: Behandlingsfeil.Avklaringsfeil) {
        behandlingsfeilCounter.labels("avklaring").inc()
        feil.uavklarteFakta.uavklarteVerdier.asNamedList().forEach { (name, fakta) ->
            if (fakta is Vurdering.Uavklart) {
                log.info("$name er uavklart fordi ${fakta.årsak}: ${fakta.begrunnelse}")

                avklaringsfeilCounter.labels(name).inc()
                influxMetricReporter.sendDataPoint(DataPoint(
                        name = "avklaringsfeil.event",
                        fields = mapOf(
                                "soknadId" to feil.uavklarteFakta.originalSøknad.id
                        ),
                        tags = mapOf(
                                "datum" to name,
                                "aarsak" to fakta.årsak.name
                        )
                ))
            }
        }
        influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf(
                "soknadId" to feil.uavklarteFakta.originalSøknad.id
        ), mapOf(
                "steg" to "avklaring",
                "type" to feil.uavklarteFakta.originalSøknad.type
        ))
        log.info("Søknad for aktør ${feil.uavklarteFakta.originalSøknad.aktorId} med id ${feil.uavklarteFakta.originalSøknad.id} er uavklart")
    }

    fun vilkårsPrøvingsFeil(feil: Behandlingsfeil.Vilkårsprøvingsfeil) {
        behandlingsfeilCounter.labels("vilkarsproving").inc()
        influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf(
                "soknadId" to feil.vilkårsprøving.originalSøknad.id
        ), mapOf(
                "steg" to "vilkarsproving",
                "type" to feil.vilkårsprøving.originalSøknad.type
        ))
        log.info("Søknad for aktør ${feil.vilkårsprøving.originalSøknad.aktorId} med id ${feil.vilkårsprøving.originalSøknad.id} oppfyller ikke vilkårene")
    }

    fun beregningsfeil(feil: Behandlingsfeil.Beregningsfeil) {
        behandlingsfeilCounter.labels("beregning").inc()
        influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf(
                "soknadId" to feil.vilkårsprøving.originalSøknad.id
        ), mapOf(
                "steg" to "beregning",
                "type" to feil.vilkårsprøving.originalSøknad.type
        ))
    }

    fun vedtakBehandlet(vedtak: SykepengeVedtak) {
        influxMetricReporter.sendDataPoint("behandling.event", mapOf(
                "soknadId" to vedtak.originalSøknad.id
        ), mapOf(
                "type" to vedtak.originalSøknad.type
        ))
        log.info("Søknad for aktør ${vedtak.originalSøknad.aktorId} med id ${vedtak.originalSøknad.id} behandlet OK.")
    }


    fun behandlingOk() = behandlingsCounter.labels("ok").inc()
    fun behandlingFeil() = behandlingsCounter.labels("feil").inc()

}


