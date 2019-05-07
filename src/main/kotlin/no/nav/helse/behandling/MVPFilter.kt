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
            vurderMVPKriterierForAndreYtelser(faktagrunnlag.arbeidInntektYtelse.ytelser)
    )

    return if (mvpKriterier.any { it != null }) {
        Either.Left(Behandlingsfeil.mvpFilter(originalSøknad.id, originalSøknad.type, mvpKriterier.filterNotNull()))
    } else {
        Either.Right(this)
    }
}
