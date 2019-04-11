package no.nav.helse.fastsetting

import no.nav.helse.behandling.FaktagrunnlagResultat
import no.nav.helse.domain.Arbeidsforhold

fun vurderArbeidsforhold(fakta : FaktagrunnlagResultat) : Vurdering<Boolean, List<Arbeidsforhold>> {
    val arbeidsforholdFakta = fakta.faktagrunnlag.arbeidsforhold

    return when {
        arbeidsforholdFakta.any {
            it.type != "Arbeidstaker"
        } -> Vurdering.Uavklart(Vurdering.Uavklart.Årsak.FALLER_UTENFOR_MVP, "Søker har frilansarbeidsforhold", arbeidsforholdFakta)
        arbeidsforholdFakta.any {
            it.arbeidsgiver.identifikator == fakta.originalSøknad.arbeidsgiver.orgnummer
                    && it.sluttdato == null
        } -> Vurdering.Avklart(true, "Søker har et aktivt arbeidsforhold hos ${fakta.originalSøknad.arbeidsgiver.navn}", arbeidsforholdFakta, "SPA")
        else -> Vurdering.Uavklart(Vurdering.Uavklart.Årsak.FALLER_UTENFOR_MVP, "Søker har ikke aktivt arbeidsforhold hos ${fakta.originalSøknad.arbeidsgiver.navn}", arbeidsforholdFakta)
    }
}
