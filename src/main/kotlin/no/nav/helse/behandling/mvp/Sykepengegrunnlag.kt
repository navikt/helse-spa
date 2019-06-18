package no.nav.helse.behandling.mvp

import no.nav.helse.behandling.søknad.Søknadsperiode
import no.nav.helse.fastsetting.yearMonth
import no.nav.helse.oppslag.Inntekt
import no.nav.helse.sykepenger.beregning.longValueExact
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

fun vurderMVPKriterierForSykepengegrunnlaget(førsteSykdomsdag: LocalDate, perioder: List<Søknadsperiode>, beregningsgrunnlag: List<Inntekt>, sammenligningsgrunnlag: List<Inntekt>): List<MVPFeil> {
    val feil = mutableListOf<MVPFeil>()

    if (perioder.isEmpty()) {
        feil.add(MVPFeil("Ingen sykdomsperioder", "Søknaden inneholder ingen sykdomsperioder"))
    } else {
        if (perioder.size > 1) {
            feil.add(MVPFeil("Mer enn én sykdomsperiode", "Søknaden inneholder mer enn én sykdomsperiode"))
        }

        if (perioder[0].fom != førsteSykdomsdag) {
            feil.add(MVPFeil("Periode og sykdom har forskjellig start", "Første dag i perioden (${perioder[0].fom}) er ikke den samme som første sykdomsdag ($førsteSykdomsdag)"))
        }
    }

    val fordeltEtterMåned = beregningsgrunnlag.distinctBy {
        it.utbetalingsperiode
    }

    if (fordeltEtterMåned.size != 3) {
        feil.add(MVPFeil("Ikke tre måneder med inntekter", "Vi forventer inntekter for hver at de tre månedene før sykdomsperioden, men vi har inntekter for ${fordeltEtterMåned.size} måneder"))
    }

    val enMånedFør = førsteSykdomsdag.minusMonths(1)
            .yearMonth()
    val treMånederFør = førsteSykdomsdag.minusMonths(3)
            .yearMonth()

    val aktuellMånedsinntekt = beregningsgrunnlag
            .filter { it.utbetalingsperiode >= treMånederFør && it.utbetalingsperiode <= enMånedFør }
            .fold(BigDecimal.ZERO) { acc, current ->
                acc.add(current.beløp)
            }
            .divide(BigDecimal.valueOf(3), RoundingMode.HALF_UP)
            .longValueExact(RoundingMode.HALF_UP)

    val tolvMånederFør = førsteSykdomsdag.minusMonths(12)
            .yearMonth()

    val sammenligningsgrunnlag = sammenligningsgrunnlag
            .filter { it.utbetalingsperiode >= tolvMånederFør && it.utbetalingsperiode <= enMånedFør }
            .fold(BigDecimal.ZERO) { acc, current ->
                acc.add(current.beløp)
            }
            .longValueExact(RoundingMode.HALF_UP)

    val omregnetÅrsinntekt = aktuellMånedsinntekt * 12
    val avvik = Math.abs(omregnetÅrsinntekt - sammenligningsgrunnlag) / sammenligningsgrunnlag.toDouble()

    if (avvik > 0.25) {
        feil.add(MVPFeil("> 25 % avvik", "Avvik på ${(avvik * 10000).toInt() / 100.00} %"))
    }

    return feil
}
