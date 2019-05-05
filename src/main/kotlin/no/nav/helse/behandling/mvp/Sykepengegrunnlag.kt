package no.nav.helse.behandling.mvp

import no.nav.helse.behandling.søknad.Søknadsperiode
import no.nav.helse.oppslag.Inntekt
import java.time.LocalDate

fun vurderMVPKriterierForSykepengegrunnlaget(førsteSykdomsdag: LocalDate, perioder: List<Søknadsperiode>, beregningsgrunnlag: List<Inntekt>): MVPFeil? {
    if (perioder.size > 1) {
        return MVPFeil("Mer enn én sykdomsperiode", "Søknaden inneholder mer enn én sykdomsperiode")
    }
    if (perioder[0].fom != førsteSykdomsdag) {
        return MVPFeil("Periode og sykdom har forskjellig start", "Første dag i perioden (${perioder[0].fom}) er ikke den samme som første sykdomsdag ($førsteSykdomsdag)")
    }

    val fordeltEtterMåned = beregningsgrunnlag.distinctBy {
        it.utbetalingsperiode
    }

    if (fordeltEtterMåned.size != 3) {
        return MVPFeil("Ikke tre måneder med inntekter", "Vi forventer inntekter for hver at de tre månedene før sykdomsperioden, men vi har inntekter for ${fordeltEtterMåned.size} måneder")
    }

    return null
}
