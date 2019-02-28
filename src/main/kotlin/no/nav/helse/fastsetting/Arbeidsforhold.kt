package no.nav.helse.fastsetting

import no.nav.helse.ArbeidsforholdFakta
import no.nav.helse.FaktagrunnlagResultat
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat

fun vurderArbeidsforhold(fakta : FaktagrunnlagResultat) : Vurdering<Boolean, ArbeidsforholdFakta> {

    val orgnummer= fakta.originalSøknad.arbeidsgiver.orgnummer
    val arbeidsforholdFakta = fakta.faktagrunnlag.arbeidsforhold
    val evaluering = evaluerArbeidsforhold(orgnummer, arbeidsforholdFakta)

    return when (evaluering.resultat) {
        Resultat.KANSKJE -> Vurdering.Uavklart(Vurdering.Uavklart.Årsak.KREVER_SKJØNNSMESSIG_VURDERING, evaluering.begrunnelse, arbeidsforholdFakta)
        else -> Vurdering.Avklart(evaluering.resultat == Resultat.JA, evaluering.begrunnelse, arbeidsforholdFakta, "SPA")
    }

}

fun evaluerArbeidsforhold(orgnummer: String, fakta: ArbeidsforholdFakta): Evaluering =
        when {
            fakta.arbeidsgivere.size > 1 -> Evaluering.kanskje("Søker har flere arbeidsgivere, systemet støtter ikke dette enda")
            fakta.arbeidsgivere[0].organisasjonsnummer == orgnummer -> Evaluering.ja("Søker har en arbeidsgiver med orgnummer $orgnummer")
            else -> Evaluering.nei("Søker har ikke en arbeidsgiver med orgnummer $orgnummer")
        }
