package no.nav.helse.fastsetting

import no.nav.helse.behandling.FaktagrunnlagResultat
import no.nav.helse.fastsetting.Vurdering.Uavklart
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsforholdDTO

fun vurderArbeidsforhold(fakta : FaktagrunnlagResultat) : Vurdering<ArbeidsforholdDTO, List<ArbeidsforholdDTO>> {
    val arbeidInntektYtelse = fakta.faktagrunnlag.arbeidInntektYtelse

    val inntekterUtenPotensielleArbeidsforhold = arbeidInntektYtelse.inntekter.filter { inntektMedArbeidsforholdDTO ->
        inntektMedArbeidsforholdDTO.muligeArbeidsforhold.isEmpty()
    }

    if (inntekterUtenPotensielleArbeidsforhold.isNotEmpty()) {
        return Uavklart(Uavklart.Årsak.FORSTÅR_IKKE_DATA, "Inntekter uten arbeidsforhold", "Søker har ${inntekterUtenPotensielleArbeidsforhold.size} inntekter uten et potensielt arbeidsforhold", emptyList())
    }

    val inntekterMedFlereMuligeArbeidsforhold = arbeidInntektYtelse.inntekter.filter { inntektMedArbeidsforholdDTO ->
        inntektMedArbeidsforholdDTO.muligeArbeidsforhold.size > 1
    }

    if (inntekterMedFlereMuligeArbeidsforhold.isNotEmpty()) {
        return Uavklart(Uavklart.Årsak.FORSTÅR_IKKE_DATA, "Inntekter med flere mulige arbeidsforhold", "Søker har ${inntekterMedFlereMuligeArbeidsforhold.size} inntekter hvor vi er usikre på hvilket arbeidsforhold det gjelder for", emptyList())
    }

    val forskjelligeArbeidsforhold = arbeidInntektYtelse.inntekter.flatMap { inntektMedArbeidsforholdDTO ->
        inntektMedArbeidsforholdDTO.muligeArbeidsforhold
    }.distinct()

    if (forskjelligeArbeidsforhold.isEmpty()) {
        return Uavklart(Uavklart.Årsak.HAR_IKKE_DATA, "Ingen inntekter", "Kan ikke avklare arbeidsforhold fordi det finnes ikke inntekter i perioden", emptyList())
    }
    if (forskjelligeArbeidsforhold.size > 1) {
        return Uavklart(Uavklart.Årsak.FORSTÅR_IKKE_DATA, "Flere arbeidsforhold med inntekt", "Søker har ${forskjelligeArbeidsforhold.size} forskjellige arbeidsforhold som han eller hun har mottatt inntekt på", forskjelligeArbeidsforhold)
    }

    if (forskjelligeArbeidsforhold[0].arbeidsgiver.identifikator != fakta.originalSøknad.arbeidsgiver.orgnummer) {
        return Uavklart(Uavklart.Årsak.HAR_IKKE_DATA, "Ingen inntekter hos aktuell arbeidsgiver", "Søker har ingen inntekter hos aktuell arbeidsgiver", forskjelligeArbeidsforhold)
    }

    val arbeidsforholdFakta = fakta.faktagrunnlag.arbeidInntektYtelse.arbeidsforhold
    return when {
        arbeidsforholdFakta.none {
            it.arbeidsgiver.identifikator == fakta.originalSøknad.arbeidsgiver.orgnummer
        } -> Uavklart(Uavklart.Årsak.HAR_IKKE_DATA, "Ingen arbeidsforhold hos aktuell arbeidsgiver", "Søker har ikke arbeidsforhold hos ${fakta.originalSøknad.arbeidsgiver.navn}", arbeidsforholdFakta)
        else -> Vurdering.Avklart(arbeidsforholdFakta.first { it.arbeidsgiver.identifikator == fakta.originalSøknad.arbeidsgiver.orgnummer }, "Søker har et arbeidsforhold hos ${fakta.originalSøknad.arbeidsgiver.navn}", arbeidsforholdFakta, "SPA")
    }
}
