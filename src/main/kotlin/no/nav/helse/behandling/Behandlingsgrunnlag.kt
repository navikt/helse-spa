package no.nav.helse.behandling

import no.nav.nare.core.evaluations.Evaluering

data class Behandlingsgrunnlag(
        val sakskompleks: Sakskompleks,
        val faktagrunnlag: Faktagrunnlag,
        val avklarteVerdier: AvklarteVerdier,
        val vilkårsprøving: Evaluering
)
