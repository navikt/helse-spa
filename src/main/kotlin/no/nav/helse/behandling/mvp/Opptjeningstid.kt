package no.nav.helse.behandling.mvp

import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsforholdDTO

fun vurderMVPKriterierForOpptjeningstid(arbeidsforhold: List<ArbeidsforholdDTO>): MVPFeil? {
    if (arbeidsforhold.size != 1) {
        return MVPFeil("For mange arbeidsforhold", "Søker har ${arbeidsforhold.size} arbeidsforhold og vi forventer kun 1")
    } else {
        return arbeidsforhold[0].sluttdato?.let {
            MVPFeil("Kun avsluttet arbeidsforhold", "Søker har ett arbeidsforhold som han eller hun har avsluttet")
        }
    }
}
