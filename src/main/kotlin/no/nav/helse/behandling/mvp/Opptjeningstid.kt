package no.nav.helse.behandling.mvp

import no.nav.helse.behandling.søknad.ArbeidsgiverFraSøknad
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsforholdDTO

fun vurderMVPKriterierForOpptjeningstid(arbeidsgiverFraSøknad: ArbeidsgiverFraSøknad, arbeidsforhold: List<ArbeidsforholdDTO>): List<MVPFeil> {
    val feil = mutableListOf<MVPFeil>()

    if (arbeidsforhold.isEmpty()) {
        feil.add(MVPFeil("Ingen arbeidsforhold", "Søker har ingen arbeidsforhold og vi forventer kun 1"))
    } else {
        if (arbeidsforhold.size != 1) {
            feil.add(MVPFeil("Mer enn ett arbeidsforhold", "Søker har ${arbeidsforhold.size} arbeidsforhold og vi forventer kun 1"))
        }

        val antall = arbeidsforhold.filter { arbeidsgiverFraSøknad.orgnummer == it.arbeidsgiver.identifikator }.size

        if (antall == 0) {
            feil.add(MVPFeil("Finner ikke arbeidsforhold fra søknad", "Finner ikke arbeidsforholdet søker har opplyst om i søknaden i listen over registrerte arbeidsforhold"))
        } else if (antall > 1) {
            feil.add(MVPFeil("Flere arbeidsforhold hos arbeidsgiver i søknad", "Søker har flere arbeidsforhold i samme virksomhet som er opplyst om i søknaden"))
        }
    }

    return feil
}
