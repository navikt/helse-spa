package no.nav.helse.behandling.mvp

import no.nav.helse.oppslag.arbeidinntektytelse.dto.YtelseDTO

fun vurderMVPKriterierForAndreYtelser(ytelser: List<YtelseDTO>): MVPFeil? {
    if (ytelser.isNotEmpty()) {
        return MVPFeil("Andre ytelser", "Søker har ${ytelser.size} ytelseutbetalinger fra det offentlige")
    }
    return null
}
