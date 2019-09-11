package no.nav.helse.behandling

import arrow.core.Either
import no.nav.helse.Behandlingsfeil
import no.nav.helse.behandling.søknad.Fraværstype
import no.nav.helse.oppslag.getGrunnbeløpForDato
import no.nav.helse.sykepenger.beregning.Beregningsgrunnlag
import no.nav.helse.sykepenger.beregning.Ferie
import no.nav.helse.sykepenger.beregning.beregn
import java.lang.RuntimeException

fun sykepengeBeregning(vilkårsprøving: Behandlingsgrunnlag): Either<Behandlingsfeil, Sykepengeberegning> =
    try {
        val beregningsresultat = beregn(lagBeregninggrunnlag(vilkårsprøving))
        val beregningsresultatFraInntektsmelding =
            beregn(lagBeregninggrunnlag(vilkårsprøving, vilkårsprøving.sakskompleks.inntektsmeldinger.firstOrNull()?.årsinntekt))

        Either.Right(
            Sykepengeberegning(
                sakskompleks = vilkårsprøving.sakskompleks,
                faktagrunnlag = vilkårsprøving.faktagrunnlag,
                avklarteVerdier = vilkårsprøving.avklarteVerdier,
                vilkårsprøving = vilkårsprøving.vilkårsprøving,
                beregning = beregningsresultat,
                beregningFraInntektsmelding = beregningsresultatFraInntektsmelding
            )
        )
    } catch (e: Exception) {
        Either.Left(Behandlingsfeil.beregningsfeil(vilkårsprøving, e))
    }

private fun lagBeregninggrunnlag(vilkårsprøving: Behandlingsgrunnlag, inntektFraInntektsmeldingen: Long? = null): Beregningsgrunnlag =
    Beregningsgrunnlag(
        fom = vilkårsprøving.sakskompleks.inntektsmeldinger.flatMap { it.arbeidsgiverperioder }.maxBy { it.tom }?.tom?.plusDays(
            1
        ) ?: vilkårsprøving.sakskompleks.søknader[0].fom, //TODO: Hvordan håndtere at vi mangler inntektsmeldingen?
        ferie = vilkårsprøving.sakskompleks.søknader[0].fravær.filter { it.type == Fraværstype.FERIE }.map { Ferie(it.fom, it.tom ?: throw RuntimeException("Ferie mangler tom-dato")) },
        permisjon = emptyList(),
        sykmeldingsgrad = vilkårsprøving.sakskompleks.søknader[0].soknadsperioder.let {
            if (it.size == 1) it[0].sykmeldingsgrad else throw Exception("takler bare én periode per nå")
        },
        sykepengegrunnlag = no.nav.helse.sykepenger.beregning.Sykepengegrunnlag(
            fastsattInntekt = inntektFraInntektsmeldingen
                ?: vilkårsprøving.avklarteVerdier.sykepengegrunnlag.fastsattVerdi.sykepengegrunnlagNårTrygdenYter.fastsattVerdi,
            grunnbeløp = getGrunnbeløpForDato(vilkårsprøving.sakskompleks.søknader[0].fom)
        ),
        sisteUtbetalingsdato = (vilkårsprøving.avklarteVerdier.maksdato.fastsattVerdi).let {
            if (it.isBefore(vilkårsprøving.sakskompleks.søknader[0].tom)) it else vilkårsprøving.sakskompleks.søknader[0].tom
        })

