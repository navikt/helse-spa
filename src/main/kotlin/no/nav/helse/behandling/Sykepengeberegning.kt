package no.nav.helse.behandling

import no.nav.helse.sykepenger.beregning.Beregningsresultat
import no.nav.nare.core.evaluations.Evaluering

data class Sykepengeberegning(
        val sakskompleks: Sakskompleks,
        val faktagrunnlag: Faktagrunnlag,
        val avklarteVerdier: AvklarteVerdier,
        val vilkårsprøving: Evaluering,
        val beregning: Beregningsresultat,
        val beregningFraInntektsmelding: Beregningsresultat)
