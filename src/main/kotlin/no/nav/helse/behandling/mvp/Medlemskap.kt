package no.nav.helse.behandling.mvp

import no.nav.helse.behandling.Tpsfakta
import no.nav.helse.fastsetting.bosattstatus
import no.nav.helse.fastsetting.landskodeNORGE

fun vurderMVPKriterierForMedlemskap(fakta: Tpsfakta): List<MVPFeil> {
    val feil = mutableListOf<MVPFeil>()

    if (fakta.statsborgerskap != landskodeNORGE) {
        feil.add(MVPFeil("Ikke norsk statsborger", "Søker må ha norsk statsborgerskap"))
    }

    if (fakta.status != bosattstatus) {
        feil.add(MVPFeil("Ikke bosatt", "Søker er ikke bosatt"))
    }

    if (fakta.bostedland != landskodeNORGE) {
        feil.add(MVPFeil("Ikke bosatt i Norge", "Søker må være bosatt i Norge"))
    }

    if (fakta.diskresjonskode != null) {
        feil.add(MVPFeil("Har diskresjonskode", "Søker har diskresjonskode"))
    }

    return feil
}
