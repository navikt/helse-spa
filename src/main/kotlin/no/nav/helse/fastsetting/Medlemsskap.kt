package no.nav.helse.fastsetting

import no.nav.helse.FaktagrunnlagResultat
import no.nav.helse.narePrometheus
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import no.nav.nare.core.specifications.Spesifikasjon

internal val toBeDecided = Spesifikasjon<Medlemsskapgrunnlag>(
        beskrivelse = "Vi har ikke nok informasjon til å kunne gi et entydig svar.",
        identitet = "Ufullstendig informasjonsgrunnlag") { Evaluering.kanskje("Vi har ikke nok informasjon til å kunne gi et entydig svar.") }

internal val boddeINorgeISykdomsperioden = Spesifikasjon<Medlemsskapgrunnlag>(
        beskrivelse = "Bodde søker i Norge da han eller hun ble syk?",
        identitet = "§ 2-1 første ledd") { søkerBorINorge(bostedsland) }

internal val harOppfyltMedlemskap = (boddeINorgeISykdomsperioden eller toBeDecided).med(
        beskrivelse = "Oppfyller søker krav om medlemsskap?",
        identitet = "Kapittel 2. Medlemskap"
)

fun søkerBorINorge(bostedland: String) =
        if (bostedland == "NOR") {
            Evaluering.ja("Søker er bosatt i Norge.")
        } else {
            Evaluering.nei("Søker er ikke bostatt i Norge.")
        }

data class Medlemsskapgrunnlag(val bostedsland: String)

fun vurderMedlemskap(soknad: FaktagrunnlagResultat): Vurdering<Boolean, Medlemsskapgrunnlag> {
    val grunnlag = Medlemsskapgrunnlag(soknad.faktagrunnlag.tps.bostedland)
    val evaluering: Evaluering = with(harOppfyltMedlemskap) {
        evaluer(grunnlag).also {
            narePrometheus.tellEvaluering { it }
        }
    }

    return when (evaluering.resultat) {
        Resultat.KANSKJE -> Vurdering.Uavklart(Vurdering.Uavklart.Årsak.KREVER_SKJØNNSMESSIG_VURDERING, evaluering.begrunnelse, grunnlag)
        else -> Vurdering.Avklart(evaluering.resultat == Resultat.JA, evaluering.begrunnelse, grunnlag, "SPA")
    }
}
