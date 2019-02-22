package no.nav.helse

import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat

fun vurderArbeidsforhold(soknad : BeriketSykepengesøknad) : Vurdering<Boolean, ArbeidsforholdFakta> {

    val orgnummer= soknad.originalSoknad.arbeidsgiver.orgnummer
    val fakta = soknad.faktagrunnlag.arbeidsforhold
    val evaluering = evaluerArbeidsforhold(orgnummer, fakta)

    return when (evaluering.resultat) {
        Resultat.KANSKJE -> Vurdering.Uavklart(Vurdering.Uavklart.Arsak.SKJONN, evaluering.begrunnelse, fakta)
        else -> Vurdering.Avklart(evaluering.resultat == Resultat.JA, evaluering.begrunnelse, fakta, "SPA")
    }

}

fun evaluerArbeidsforhold(orgnummer: String, fakta: ArbeidsforholdFakta): Evaluering =
        when {
            fakta.arbeidsgivere.size > 1 -> Evaluering.kanskje("Søker har flere arbeidsgivere, systemet støtter ikke dette enda")
            fakta.arbeidsgivere[0].organisasjonsnummer == orgnummer -> Evaluering.ja("Søker har en arbeidsgiver med orgnummer $orgnummer")
            else -> Evaluering.nei("Søker har ikke en arbeidsgiver med orgnummer $orgnummer")
        }
