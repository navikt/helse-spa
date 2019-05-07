package no.nav.helse.behandling.mvp

import no.nav.helse.behandling.søknad.ArbeidsgiverFraSøknad
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsforholdDTO

fun vurderMVPKriterierForOpptjeningstid(arbeidsgiverFraSøknad: ArbeidsgiverFraSøknad, arbeidsforhold: List<ArbeidsforholdDTO>): MVPFeil? {
    return when {
        arbeidsforhold.size != 1 -> MVPFeil("Mer enn ett arbeidsforhold", "Søker har ${arbeidsforhold.size} arbeidsforhold og vi forventer kun 1")
        arbeidsgiverFraSøknad.orgnummer != arbeidsforhold[0].arbeidsgiver.identifikator -> MVPFeil("Finner ikke arbeidsforhold fra søknad", "Finner ikke arbeidsforholdet søker har opplyst om i søknaden i listen over registrerte arbeidsforhold")
        else -> null
    }
}
