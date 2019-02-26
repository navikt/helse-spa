package no.nav.helse.fastsetting

import no.nav.helse.BeriketSykepengesøknad
import no.nav.helse.narePrometheus
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import no.nav.nare.core.specifications.Spesifikasjon

val erMedlem = Spesifikasjon<Medlemsskapgrunnlag>(
        beskrivelse = "Formålet med sykepenger er å gi kompensasjon for bortfall av arbeidsinntekt for yrkesaktive medlemmer som er arbeidsuføre på grunn av sykdom eller skade.",
        identitet = "§ 8-1"
) { grunnlag-> sokerBorI("NOR", grunnlag) }

fun sokerBorI(land: String, grunnlag: Medlemsskapgrunnlag): Evaluering =
        if (grunnlag.bostedsland == land)
            Evaluering.ja("Søker er bosatt i $land")
        else
            Evaluering.nei("Søker er bosatt i ${grunnlag.bostedsland} som ikke er $land")

data class Medlemsskapgrunnlag(val bostedsland: String)

fun vurderMedlemskap(soknad: BeriketSykepengesøknad): Vurdering<Boolean, Medlemsskapgrunnlag> {
    val grunnlag = Medlemsskapgrunnlag(soknad.faktagrunnlag.tps.bostedland)
    val evaluering: Evaluering = with(erMedlem) {
        evaluer(grunnlag).also {
            narePrometheus.tellEvaluering { it }
        }
    }

    return when (evaluering.resultat) {
        Resultat.KANSKJE -> Vurdering.Uavklart(Vurdering.Uavklart.Årsak.KREVER_SKJØNNSMESSIG_VURDERING, evaluering.begrunnelse, grunnlag)
        else -> Vurdering.Avklart(evaluering.resultat == Resultat.JA, evaluering.begrunnelse, grunnlag, "SPA")
    }
}
