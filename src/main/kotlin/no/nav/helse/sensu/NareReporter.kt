package no.nav.helse.sensu

import no.nav.nare.core.evaluations.Evaluering
import java.util.*
import java.util.Arrays.asList

class NareReporter(sensuClient: SensuClient) {

    private val reporter = InfluxMetricReporter(sensuClient, "spa-events", mapOf(
            "application" to (System.getenv("NAIS_APP_NAME") ?: "spa"),
            "cluster" to (System.getenv("NAIS_CLUSTER_NAME") ?: "dev-fss"),
            "namespace" to (System.getenv("NAIS_NAMESPACE") ?: "default")
    ))


    fun toDatapoints(eval: Evaluering): List<DataPoint> {
        val uuid = UUID.randomUUID().toString()
        return flatten(eval).map {
            DataPoint(
                    "spa.vilkarsproving",
                    mapOf("beskrivelse" to it.beskrivelse),
                    mapOf(
                            "uuid" to uuid,
                            "identifikator" to it.identifikator,
                            "resultat" to it.resultat.name))
        }
    }

    private fun flatten(eval: Evaluering): List<Evaluering> {
        return asList(eval) + eval.children.flatMap { flatten(it) }
    }

    fun gjennomførtVilkårsprøving(vilkårsprøving: Evaluering) {
        toDatapoints(vilkårsprøving).forEach {
            reporter.sendDataPoint(it)
        }

    }
}
