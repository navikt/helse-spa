package no.nav.helse.behandling

import arrow.core.Either
import no.nav.helse.Behandlingsfeil
import no.nav.helse.behandling.mvp.*

fun FaktagrunnlagResultat.mvpFilter(): Either<Behandlingsfeil, FaktagrunnlagResultat> {
    val mvpKriterier = listOf(
            sjekkSvarISøknaden(sakskompleks.søknader[0]),
            vurderMVPKriterierForMedlemskap(faktagrunnlag.tps),
            vurderMVPKriterierForOpptjeningstid(sakskompleks.søknader[0].arbeidsgiver, faktagrunnlag.arbeidInntektYtelse.arbeidsforhold),
            vurderMVPKriterierForSykepengegrunnlaget(sakskompleks.søknader[0].startSyketilfelle, sakskompleks.søknader[0].soknadsperioder, faktagrunnlag.beregningsperiode, faktagrunnlag.sammenligningsperiode),
            vurderMVPKriterierForAndreYtelser(faktagrunnlag.arbeidInntektYtelse.ytelser, faktagrunnlag.ytelser)
    )

    val mvpFeil = mvpKriterier.filter { it.isNotEmpty() }.flatMap { it }

    return if (mvpFeil.isNotEmpty()) {
        Either.Left(Behandlingsfeil.mvpFilter(sakskompleks, mvpFeil))
    } else {
        Either.Right(this)
    }
}
