package no.nav.helse.fastsetting

import no.nav.helse.behandling.FaktagrunnlagResultat
import no.nav.helse.domain.Arbeidsforhold
import no.nav.helse.fastsetting.Vurdering.Uavklart

fun vurderArbeidsforhold(fakta : FaktagrunnlagResultat) : Vurdering<Boolean, List<Arbeidsforhold>> {
    val arbeidsforholdFakta = fakta.faktagrunnlag.arbeidsforhold

    return when {
        arbeidsforholdFakta.any {
            it.type != "Arbeidstaker"
        } -> Uavklart(Uavklart.Årsak.FALLER_UTENFOR_MVP, "Søker har frilansarbeidsforhold", arbeidsforholdFakta)
        arbeidsforholdFakta.none {
            it.arbeidsgiver.identifikator == fakta.originalSøknad.arbeidsgiver.orgnummer
        } -> Uavklart(Uavklart.Årsak.HAR_IKKE_DATA, "Søker har ikke arbeidsforhold hos ${fakta.originalSøknad.arbeidsgiver.navn}", arbeidsforholdFakta)
        arbeidsforholdFakta.first {
            it.arbeidsgiver.identifikator == fakta.originalSøknad.arbeidsgiver.orgnummer
        }.sluttdato != null -> Uavklart(Uavklart.Årsak.FALLER_UTENFOR_MVP, "Søker har sluttdato hos ${fakta.originalSøknad.arbeidsgiver.navn}", arbeidsforholdFakta)
        else -> Vurdering.Avklart(true, "Søker har et aktivt arbeidsforhold hos ${fakta.originalSøknad.arbeidsgiver.navn}", arbeidsforholdFakta, "SPA")
    }
}
