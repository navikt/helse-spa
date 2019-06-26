package no.nav.helse.behandling

import no.nav.helse.behandling.søknad.Sykepengesøknad
import no.nav.helse.sykepenger.beregning.Beregningsresultat
import no.nav.nare.core.evaluations.Evaluering
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class SykepengeVedtak(
        val originalSøknad: Sykepengesøknad,
        val faktagrunnlag: Faktagrunnlag,
        val avklarteVerdier: AvklarteVerdier,
        val vilkårsprøving: Evaluering,
        val beregning: Beregningsresultat,
        val vedtak: Vedtak,
        val behandlingsId: String = UUID.randomUUID().toString()
)

data class Vedtak(val perioder: List<Vedtaksperiode> = emptyList())
data class Vedtaksperiode(
        val fom: LocalDate,
        val tom: LocalDate,
        val dagsats: BigDecimal,
        val grad: Int = 100,
        val fordeling: List<Fordeling>
)
data class Fordeling(
        val mottager: String,
        val andel: Int
)
