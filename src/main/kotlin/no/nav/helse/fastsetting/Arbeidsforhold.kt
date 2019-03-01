package no.nav.helse.fastsetting

import no.nav.helse.behandling.*
import no.nav.helse.domain.*
import no.nav.nare.core.evaluations.*

fun vurderArbeidsforhold(fakta : FaktagrunnlagResultat) : Vurdering<Boolean, List<Arbeidsforhold>> {

    val orgnummer= fakta.originalSøknad.arbeidsgiver.orgnummer
    val arbeidsforholdFakta = fakta.faktagrunnlag.arbeidsforhold
    val evaluering = evaluerArbeidsforhold(orgnummer, arbeidsforholdFakta)

    return when (evaluering.resultat) {
        Resultat.KANSKJE -> Vurdering.Uavklart(Vurdering.Uavklart.Årsak.KREVER_SKJØNNSMESSIG_VURDERING, evaluering.begrunnelse, arbeidsforholdFakta)
        else -> Vurdering.Avklart(evaluering.resultat == Resultat.JA, evaluering.begrunnelse, arbeidsforholdFakta, "SPA")
    }

}

fun evaluerArbeidsforhold(orgnummer: String, fakta: List<Arbeidsforhold>): Evaluering =
        when {
            fakta.size > 1 -> Evaluering.kanskje("Søker har flere arbeidsgivere, systemet støtter ikke dette enda")
            fakta[0].arbeidsgiver.orgnummer == orgnummer -> Evaluering.ja("Søker har en arbeidsgiver med orgnummer $orgnummer")
            else -> Evaluering.nei("Søker har ikke en arbeidsgiver med orgnummer $orgnummer")
        }
