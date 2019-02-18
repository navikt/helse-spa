package no.nav.helse

import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import no.nav.nare.core.specifications.Spesifikasjon

val erMedlem = Spesifikasjon<Tpsfakta>(
        beskrivelse = "Formålet med sykepenger er å gi kompensasjon for bortfall av arbeidsinntekt for yrkesaktive medlemmer som er arbeidsuføre på grunn av sykdom eller skade.",
        identitet = "§ 8-1"
) { tps -> sokerBorI("NOR", tps) }

fun sokerBorI(land: String, tps: Tpsfakta): Evaluering =
        if (tps.bostedland == land)
            Evaluering.ja("Søker er bosatt i $land")
        else
            Evaluering.nei("Søker er bosatt i ${tps.bostedland} som ikke er $land")

fun vurderMedlemskap(soknad: BeriketSykepengesoknad): Vurdering<Boolean, Tpsfakta> {
    val evaluering = narePrometheus.tellEvaluering { erMedlem.evaluer(soknad.faktagrunnlag.tps) }

    return when (evaluering.resultat) {
        Resultat.KANSKJE -> Vurdering.Uavklart(Vurdering.Uavklart.Arsak.SKJONN, evaluering.begrunnelse, soknad.faktagrunnlag.tps)
        else -> Vurdering.Avklart(evaluering.resultat == Resultat.JA, evaluering.begrunnelse, soknad.faktagrunnlag.tps
                , "SPA")
    }
}
