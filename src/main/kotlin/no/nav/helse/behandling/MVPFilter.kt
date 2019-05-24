package no.nav.helse.behandling

import arrow.core.Either
import no.nav.helse.Behandlingsfeil
import no.nav.helse.behandling.mvp.*

fun FaktagrunnlagResultat.mvpFilter(): Either<Behandlingsfeil, FaktagrunnlagResultat> {
    val mvpKriterier = listOf(
            sjekkSvarISøknaden(originalSøknad),
            vurderMVPKriterierForMedlemskap(faktagrunnlag.tps),
            vurderMVPKriterierForOpptjeningstid(originalSøknad.arbeidsgiver, faktagrunnlag.arbeidInntektYtelse.arbeidsforhold),
            vurderMVPKriterierForSykepengegrunnlaget(originalSøknad.startSyketilfelle, originalSøknad.soknadsperioder, faktagrunnlag.beregningsperiode),
            vurderMVPKriterierForAndreYtelser(faktagrunnlag.arbeidInntektYtelse.ytelser, faktagrunnlag.ytelser)
    )

    val mvpFeil = mvpKriterier.filter { it.isNotEmpty() }.flatMap { it }

    return if (mvpFeil.isNotEmpty()) {
        Either.Left(Behandlingsfeil.mvpFilter(originalSøknad, mvpFeil))
    } else {
        Either.Right(this)
    }
}
