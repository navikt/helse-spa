package no.nav.helse.fastsetting

import no.nav.helse.behandling.FaktagrunnlagResultat
import no.nav.helse.fastsetting.Vurdering.Uavklart
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsforholdDTO

fun vurderArbeidsforhold(fakta : FaktagrunnlagResultat) : Vurdering<Boolean, List<ArbeidsforholdDTO>> {
    val arbeidsforholdFakta = fakta.faktagrunnlag.arbeidInntektYtelse.arbeidsforhold

    return when {
        arbeidsforholdFakta.any {
            it.type != "Arbeidstaker"
        } -> Uavklart(Uavklart.Årsak.FALLER_UTENFOR_MVP, "Freelancer","Søker har frilansarbeidsforhold", arbeidsforholdFakta)
        arbeidsforholdFakta.none {
            it.arbeidsgiver.identifikator == fakta.originalSøknad.arbeidsgiver.orgnummer
        } -> Uavklart(Uavklart.Årsak.HAR_IKKE_DATA, "Ingen arbeidsforhold hos aktuell arbeidsgiver", "Søker har ikke arbeidsforhold hos ${fakta.originalSøknad.arbeidsgiver.navn}", arbeidsforholdFakta)
        arbeidsforholdFakta.first {
            it.arbeidsgiver.identifikator == fakta.originalSøknad.arbeidsgiver.orgnummer
        }.sluttdato != null -> Uavklart(Uavklart.Årsak.FALLER_UTENFOR_MVP, "Sluttet hos aktuell arbeidsgiver", "Søker har sluttdato hos ${fakta.originalSøknad.arbeidsgiver.navn}", arbeidsforholdFakta)
        else -> Vurdering.Avklart(true, "Søker har et aktivt arbeidsforhold hos ${fakta.originalSøknad.arbeidsgiver.navn}", arbeidsforholdFakta, "SPA")
    }
}
