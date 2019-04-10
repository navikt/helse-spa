package no.nav.helse.fastsetting

import no.nav.helse.behandling.FaktagrunnlagResultat
import no.nav.helse.domain.Arbeidsforhold
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat

fun vurderArbeidsforhold(fakta : FaktagrunnlagResultat) : Vurdering<Boolean, List<Arbeidsforhold>> {

    val orgnummer= fakta.originalSøknad.arbeidsgiver.orgnummer
    val arbeidsforholdFakta = fakta.faktagrunnlag.arbeidsforhold
    val evaluering = evaluerArbeidsforhold(orgnummer, arbeidsforholdFakta)

    return when (evaluering.resultat) {
        Resultat.KANSKJE -> Vurdering.Uavklart(Vurdering.Uavklart.Årsak.KREVER_SKJØNNSMESSIG_VURDERING, evaluering.begrunnelse, arbeidsforholdFakta)
        else -> Vurdering.Avklart(evaluering.resultat == Resultat.JA, evaluering.begrunnelse, arbeidsforholdFakta, "SPA")
    }

}

val søker_har_arbeidsgiver = "Søker har en arbeidsgiver med orgnummer"

fun evaluerArbeidsforhold(orgnummer: String, fakta: List<Arbeidsforhold>): Evaluering =
        when {
            fakta.isEmpty() -> Evaluering.nei("Søker har ikke en arbeidsgiver med orgnummer $orgnummer")
            fakta.size > 1 -> Evaluering.kanskje("Søker har flere arbeidsgivere, systemet støtter ikke dette enda")
            fakta[0].arbeidsgiver.identifikator == orgnummer -> {
                Evaluering.ja(søker_har_arbeidsgiver + orgnummer)
            }
            else -> Evaluering.nei("Søker har ikke en arbeidsgiver med orgnummer $orgnummer")
        }
