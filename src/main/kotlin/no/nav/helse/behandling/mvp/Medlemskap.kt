package no.nav.helse.behandling.mvp

import no.nav.helse.behandling.Tpsfakta
import no.nav.helse.fastsetting.bosattstatus
import no.nav.helse.fastsetting.landskodeNORGE

fun vurderMVPKriterierForMedlemskap(fakta: Tpsfakta): MVPFeil? {
    return when {
        fakta.statsborgerskap != landskodeNORGE -> MVPFeil("Ikke norsk statsborger", "Søker må ha norsk statsborgerskap")
        fakta.status != bosattstatus -> MVPFeil("Ikke bosatt", "Søker er ikke bosatt")
        fakta.bostedland != landskodeNORGE -> MVPFeil("Ikke bosatt i Norge", "Søker må være bosatt i Norge")
        fakta.diskresjonskode != null -> MVPFeil("Har diskresjonskode", "Søker har diskresjonskode")
        else -> null
    }
}
