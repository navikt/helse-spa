package no.nav.helse.probe

import no.nav.nare.core.evaluations.Evaluering
import java.util.*
import java.util.Arrays.asList


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

