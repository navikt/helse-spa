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
        private val søknadFiltrertBortPgaTypeCounter = Counter.build()
            .name("soknad_filtrert_bort_pga_type")
            .labelNames("type")
            .help("Antall søknader som blir filtrert bort fordi vi ikke støtter typen")
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

    fun filtrertBortSøknadPgaType(søknadId: String, søknadType: String) {
        log.info("Søknad $søknadId er filtrert bort pga den har type: $søknadType")
        søknadFiltrertBortPgaTypeCounter.labels(søknadType).inc()
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
        with(behandlingsfeil) {
            when (this) {
                is MVPFilterFeil -> mvpFilter()
                is RegisterFeil -> registerFeil()
                is Avklaringsfeil -> avklaringsFeil()
                is Vilkårsprøvingsfeil -> vilkårsPrøvingsFeil()
                is Beregningsfeil -> beregningsfeil()
            }
        }
    }

    private fun kriterieForMVPErIkkeOppfylt(søknadId: String, feil: MVPFeil, utslagsgivende: Boolean) {
        log.info("mvp-kriterie ikke oppfylt: ${feil.årsak} - ${feil.beskrivelse}")
        influxMetricReporter.sendDataPoint("mvpfeil.event",
                mapOf(
                        "soknadId" to søknadId,
                        "beskrivelse" to feil.beskrivelse
                ),
                mapOf(
                        "aarsak" to feil.årsak,
                        "utslagsgivende" to if (utslagsgivende) "JA" else "NEI"
                ))
    }

    private fun MVPFilterFeil.mvpFilter() {
        mvpFeil.onEach {
            kriterieForMVPErIkkeOppfylt(sakskompleksId, it, mvpFeil.size == 1)
        }

        behandlingsfeilCounter.labels("mvpFilter").inc()
        influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf(
                "sakskompleksId" to sakskompleksId
        ), mapOf(
                "steg" to "mvpFilter",
                "type" to sakskompleks.søknader[0].type
        ))

        log.info("Sakskompleks for aktør ${sakskompleks.aktørId} faller ut av MVP")
    }

    private fun RegisterFeil.registerFeil() {
        log.warn("$feilmelding: ${throwable.message}", throwable)

        behandlingsfeilCounter.labels("register").inc()
        influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf(
                "sakskompleksId" to sakskompleks.id
        ), mapOf(
                "steg" to "register",
                "type" to sakskompleks.søknader[0].type
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
                                "sakskompleksId" to uavklarteFakta.sakskompleks.id
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
                "sakskompleksId" to uavklarteFakta.sakskompleks.id
        ), mapOf(
                "steg" to "avklaring",
                "type" to uavklarteFakta.sakskompleks.søknader[0].type
        ))
        log.info("Sakskompleks for aktør ${uavklarteFakta.sakskompleks.aktørId} med id ${uavklarteFakta.sakskompleks.id} er uavklart")
    }

    private fun Vilkårsprøvingsfeil.vilkårsPrøvingsFeil() {
        behandlingsfeilCounter.labels("vilkarsproving").inc()
        influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf(
                "sakskompleks" to vilkårsprøving.sakskompleks.id
        ), mapOf(
                "steg" to "vilkarsproving",
                "type" to vilkårsprøving.sakskompleks.søknader[0].type
        ))
        log.info("Sakskompleks for aktør ${vilkårsprøving.sakskompleks.aktørId} med id ${vilkårsprøving.sakskompleks.søknader[0].id} oppfyller ikke vilkårene")
    }

    private fun Beregningsfeil.beregningsfeil() {
        behandlingsfeilCounter.labels("beregning").inc()
        influxMetricReporter.sendDataPoint("behandlingsfeil.event", mapOf(
                "sakskompleksId" to vilkårsprøving.sakskompleks.id
        ), mapOf(
                "steg" to "beregning",
                "type" to vilkårsprøving.sakskompleks.søknader[0].type
        ))
        log.info(feilmelding)
    }

    fun vedtakBehandlet(vedtak: SykepengeVedtak) {
        influxMetricReporter.sendDataPoint("behandling.event", mapOf(
                "sakskompleksId" to vedtak.sakskompleks.id
        ), mapOf(
                "type" to vedtak.sakskompleks.søknader[0].type
        ))
        log.info("Sakskompleks for aktør ${vedtak.sakskompleks.aktørId} med id ${vedtak.sakskompleks.id} behandlet OK.")
    }


    fun behandlingOk() = behandlingsCounter.labels("ok").inc()
    fun behandlingFeil() = behandlingsCounter.labels("feil").inc()

}


