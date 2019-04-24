package no.nav.helse.fastsetting

import no.nav.helse.behandling.Tpsfakta
import no.nav.helse.fastsetting.Vurdering.Avklart
import no.nav.helse.fastsetting.Vurdering.Uavklart


val bosattstatus = "BOSA"
val landskodeNORGE = "NOR"
val søkerOppfyllerKravOmMedlemskap = "Søker oppfyller krav om medlemskap"

fun vurderMedlemskap(fakta: Tpsfakta): Vurdering<Boolean, Tpsfakta> {
    return when {
        fakta.statsborgerskap != landskodeNORGE -> Uavklart(Uavklart.Årsak.FALLER_UTENFOR_MVP, "Ikke norsk statsborger", "Søker må ha norsk statsborgerskap", fakta)
        fakta.status != bosattstatus -> Uavklart(Uavklart.Årsak.FALLER_UTENFOR_MVP, "Ikke bosatt", "Søker er ikke bosatt", fakta)
        fakta.bostedland != landskodeNORGE -> Uavklart(Uavklart.Årsak.FALLER_UTENFOR_MVP, "Ikke bosatt i Norge", "Søker må være bosatt i Norge", fakta)
        fakta.diskresjonskode != null -> Uavklart(Uavklart.Årsak.FALLER_UTENFOR_MVP, "Diskresjon", "Søker har diskresjonskode", fakta)
        else -> Avklart(true, søkerOppfyllerKravOmMedlemskap, fakta, "SPA")
    }
}
