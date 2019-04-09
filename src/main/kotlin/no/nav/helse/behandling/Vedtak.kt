package no.nav.helse.behandling

import no.nav.helse.Behandlingsfeil
import no.nav.helse.Either
import java.math.BigDecimal

fun vedtak(beregning: Sykepengeberegning): Either<Behandlingsfeil, SykepengeVedtak> =
        Either.Right(
                SykepengeVedtak(
                        originalSøknad = beregning.originalSøknad,
                        faktagrunnlag = beregning.faktagrunnlag,
                        vilkårsprøving = beregning.vilkårsprøving,
                        avklarteVerdier = beregning.avklarteVerdier,
                        beregning = beregning.beregning,
                        vedtak = Vedtak(perioder = beregnVedtaksperioder(beregning))
                )
        )

fun beregnVedtaksperioder(sykepengeberegning: Sykepengeberegning): List<Vedtaksperiode> {
    // én vedtaksperiode er en sammenhengende rekke dager med samme dagsats og samme fordeling av utbetaling mellom arbeidsgiver og sykemeldt
    // vi regner med én arbeidsgiver og full refusjon i MVP 1 :)
    // dermed ser vi bare på dagsats.
    return sykepengeberegning.beregning.dagsatser
            .sortedBy { it.dato }
            .fold(emptyList()) { resultSoFar, current ->
                when {
                    resultSoFar.isEmpty() -> resultSoFar.plus(Vedtaksperiode(fom = current.dato, tom = current.dato, dagsats = BigDecimal.valueOf(current.sats), fordeling = emptyList()))
                    else -> {
                        val last = resultSoFar.last()
                        when {
                            last.dagsats == BigDecimal.valueOf(current.sats) -> {
                                val chopped = resultSoFar.minus(last)
                                chopped.plus(last.copy(tom = current.dato))
                            }
                            else -> resultSoFar.plus(Vedtaksperiode(fom = current.dato, tom = current.dato, dagsats = BigDecimal.valueOf(current.sats), fordeling = emptyList()))
                        }
                    }
                }
            }
}
