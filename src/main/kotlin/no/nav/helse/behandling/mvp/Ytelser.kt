package no.nav.helse.behandling.mvp

import no.nav.helse.oppslag.arbeidinntektytelse.dto.YtelseDTO
import no.nav.helse.oppslag.arbeidinntektytelse.dto.YtelserDTO

fun vurderMVPKriterierForAndreYtelser(ytelser: List<YtelseDTO>, ytelserFraArenaOgInfotrygd: YtelserDTO): List<MVPFeil> {
    val feil = mutableListOf<MVPFeil>()
    if (ytelser.isNotEmpty()) {
        feil.add(MVPFeil("Andre ytelser", "Søker har ${ytelser.size} ytelseutbetalinger fra det offentlige"))
    }
    if (ytelserFraArenaOgInfotrygd.infotrygd.filterNot { it.sak.tema == "Sykepenger" && it.sak.ikkeStartet }.isNotEmpty()) {
        feil.add(MVPFeil("Andre ytelser (Infotrygd)", "Søker har ${ytelserFraArenaOgInfotrygd.infotrygd.size} saker i Infotrygd"))
    }
    if (ytelserFraArenaOgInfotrygd.arena.isNotEmpty()) {
        feil.add(MVPFeil("Andre ytelser (Arena)", "Søker har ${ytelserFraArenaOgInfotrygd.arena.size} saker i Arena"))
    }
    return feil
}
