package no.nav.helse.probe

import io.prometheus.client.Counter
import no.nav.helse.Behandlingsfeil
import no.nav.helse.Behandlingsfeil.*
import no.nav.helse.Environment
import no.nav.helse.SaksbehandlingStream
import no.nav.helse.behandling.SykepengeVedtak
import no.nav.helse.behandling.mvp.MVPFeil
import no.nav.helse.fastsetting.Vurdering
import no.nav.nare.core.evaluations.Evaluering
import org.slf4j.LoggerFactory

class SaksbehandlingProbe(env: Environment) {
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
                .labelNames("status", "type")
                .help("Antall søknader mottatt fordelt på status")
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

    private fun sendMottattSykepengesøknadEvent(søknadId: String, status: String, type: String) {
        influxMetricReporter.sendDataPoint("sykepengesoknad.mottatt",
                mapOf(
                        "soknadId" to søknadId
                ),
                mapOf(
                        "status" to status,
                        "type" to type
                ))
    }

    fun mottattSøknad(søknadId: String, søknadStatus: String, søknadType: String) {
        sendMottattSykepengesøknadEvent(søknadId, søknadStatus, søknadType)
        mottattCounter.labels(søknadStatus, søknadType).inc()
    }

    fun mottattSøknadUansettStatusOgType(søknadId: String) {
        mottattSøknad(søknadId, "ALLE", "ALLE")
    }

    fun mottattSøknadUansettType(søknadId: String, status: String) {
        mottattSøknad(søknadId, status, "ALLE")
    }

    fun mottattSøknadSendtNAV(søknadId: String, søknadType: String) {
        mottattSøknad(søknadId, "SENDT_NAV", søknadType)
    }

    fun gjennomførtVilkårsprøving(value: Evaluering) {

            influxMetricReporter.sendDataPoints(toDatapoints(value))
    }

    fun behandlingsFeilMedType(behandlingsfeil: Behandlingsfeil) {
        log.warn(behandlingsfeil.feilmelding)
        with(behandlingsfeil) {
            when (this) {
                is Deserialiseringsfeil -> serialiseringsFeil()
                is MVPFilterFeil -> mvpFilter()
                is RegisterFeil -> registerFeil()
                is Avklaringsfeil -> avklaringsFeil()
                is Vilkårsprøvingsfeil -> vilkårsPrøvingsFeil()
                is Beregningsfeil -> beregningsfeil()
            }
        }
    }

    private fun Deserialiseringsfeil.serialiseringsFeil() {
        behandlingsfeilCounter.labels("deserialisering").inc()
        influxMetricReporter.sendDataPoint("behandlingsfeil.event",
                mapOf(
                        "soknadId" to soknadId,
                        "feilmelding" to feilmelding),
                mapOf(
                        "steg" to "deserialisering",
                        "type" to json.get("type").asText()
                ))
    }

    private fun kriterieForMVPErIkkeOppfylt(søknadId: String, feil: MVPFeil) {
        log.info("mvp-kriterie ikke oppfylt: ${feil.årsak} - ${feil.beskrivelse}")
        influxMetricReporter.sendDataPoint("mvpfeil.event",
                mapOf(
                        "soknadId" to søknadId,
                        "beskrivelse" to feil.beskrivelse
                ),
                mapOf(
                        "aarsak" to feil.årsak

                ))
    }

    private fun MVPFilterFeil.mvpFilter() {
        mvpFeil.onEach {
            kriterieForMVPErIkkeOppfylt(soknadId, it)
        }

        behandlingsfeilCounter.labels("mvpFilter").inc()
        influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf(
                "soknadId" to soknadId
        ), mapOf(
                "steg" to "mvpFilter",
                "type" to søknadstype
        ))
    }

    private fun RegisterFeil.registerFeil() {
        log.warn(throwable.message, throwable)

        behandlingsfeilCounter.labels("register").inc()
        influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf(
                "soknadId" to søknad.id
        ), mapOf(
                "steg" to "register",
                "type" to søknad.type
        ))
    }

    private fun Avklaringsfeil.avklaringsFeil() {
        behandlingsfeilCounter.labels("avklaring").inc()
        uavklarteFakta.uavklarteVerdier.asNamedList().forEach { (name, fakta) ->
            if (fakta is Vurdering.Uavklart) {
                log.info("$name er uavklart fordi ${fakta.årsak}: ${fakta.begrunnelse}")

                avklaringsfeilCounter.labels(name).inc()
                influxMetricReporter.sendDataPoint(DataPoint(
                        name = "avklaringsfeil.event",
                        fields = mapOf(
                                "soknadId" to uavklarteFakta.originalSøknad.id
                        ),
                        tags = mapOf(
                                "datum" to name,
                                "aarsak" to fakta.årsak.name,
                                "beskrivelse" to fakta.underårsak
                        )
                ))
            }
        }



        influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf(
                "soknadId" to uavklarteFakta.originalSøknad.id
        ), mapOf(
                "steg" to "avklaring",
                "type" to uavklarteFakta.originalSøknad.type
        ))
        log.info("Søknad for aktør ${uavklarteFakta.originalSøknad.aktorId} med id ${uavklarteFakta.originalSøknad.id} er uavklart")
    }

    private fun Vilkårsprøvingsfeil.vilkårsPrøvingsFeil() {
        behandlingsfeilCounter.labels("vilkarsproving").inc()
        influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf(
                "soknadId" to vilkårsprøving.originalSøknad.id
        ), mapOf(
                "steg" to "vilkarsproving",
                "type" to vilkårsprøving.originalSøknad.type
        ))
        log.info("Søknad for aktør ${vilkårsprøving.originalSøknad.aktorId} med id ${vilkårsprøving.originalSøknad.id} oppfyller ikke vilkårene")
    }

    private fun Beregningsfeil.beregningsfeil() {
        behandlingsfeilCounter.labels("beregning").inc()
        influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf(
                "soknadId" to vilkårsprøving.originalSøknad.id
        ), mapOf(
                "steg" to "beregning",
                "type" to vilkårsprøving.originalSøknad.type
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


