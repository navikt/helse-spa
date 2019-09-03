package no.nav.helse.behandling

import arrow.core.Either
import no.nav.helse.Behandlingsfeil
import java.math.BigDecimal

fun vedtak(beregning: Sykepengeberegning): Either<Behandlingsfeil, SykepengeVedtak> =
        Either.Right(
                SykepengeVedtak(
                        sakskompleks = beregning.sakskompleks,
                        faktagrunnlag = beregning.faktagrunnlag,
                        vilkårsprøving = beregning.vilkårsprøving,
                        avklarteVerdier = beregning.avklarteVerdier,
                        beregning = beregning.beregning,
                        beregningFraInntektsmelding = beregning.beregningFraInntektsmelding,
                        vedtak = Vedtak(perioder = beregnVedtaksperioder(beregning))
                )
        )

fun beregnVedtaksperioder(sykepengeberegning: Sykepengeberegning): List<Vedtaksperiode> {
    // én vedtaksperiode er en sammenhengende rekke dager med samme dagsats og samme fordeling av utbetaling mellom arbeidsgiver og sykemeldt
    // vi regner med én arbeidsgiver og full refusjon i MVP 1 :)
    // dermed ser vi bare på dagsats.
    return sykepengeberegning.beregningFraInntektsmelding.dagsatser //Todo: velg riktig beregning
            .sortedBy { it.dato }
            .fold(emptyList()) { resultSoFar, current ->
                when {
                    resultSoFar.isEmpty() -> resultSoFar.plus(Vedtaksperiode(fom = current.dato, tom = current.dato, dagsats = BigDecimal.valueOf(current.sats), fordeling = listOf(Fordeling(mottager = sykepengeberegning.sakskompleks.orgnummer, andel = 100))))
                            else -> {
                        val last = resultSoFar.last()
                        when {
                            last.dagsats == BigDecimal.valueOf(current.sats) -> {
                                val chopped = resultSoFar.minus(last)
                                chopped.plus(last.copy(tom = current.dato))
                            }
                            else -> resultSoFar.plus(Vedtaksperiode(fom = current.dato, tom = current.dato, dagsats = BigDecimal.valueOf(current.sats), fordeling = listOf(Fordeling(mottager = sykepengeberegning.sakskompleks.orgnummer, andel = 100))))
                        }
                    }
                }
            }
}
