package no.nav.helse.behandling.mvp

import no.nav.helse.oppslag.arbeidinntektytelse.dto.YtelseDTO

fun vurderMVPKriterierForAndreYtelser(ytelser: List<YtelseDTO>): List<MVPFeil> {
    if (ytelser.isNotEmpty()) {
        return listOf(MVPFeil("Andre ytelser", "Søker har ${ytelser.size} ytelseutbetalinger fra det offentlige"))
    }
    return emptyList()
}
