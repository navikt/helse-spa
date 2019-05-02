package no.nav.helse.behandling.mvp

import no.nav.helse.domain.ArbeidsgiverFraSøknad
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsforholdDTO

fun vurderMVPKriterierForArbeidsforhold(arbeidsgiverFraSøknad: ArbeidsgiverFraSøknad, arbeidsforhold: List<ArbeidsforholdDTO>): MVPFeil? {
    return when {
        arbeidsforhold.any {
            it.type != "Arbeidstaker"
        } -> MVPFeil("Frilans arbeidsforhold", "Søker har frilansarbeidsforhold")
        arbeidsforhold.first {
            it.arbeidsgiver.identifikator == arbeidsgiverFraSøknad.orgnummer
        }.sluttdato != null -> MVPFeil("Sluttet hos aktuell arbeidsgiver", "Søker har sluttdato hos ${arbeidsgiverFraSøknad.navn}")
        else -> null
    }
}