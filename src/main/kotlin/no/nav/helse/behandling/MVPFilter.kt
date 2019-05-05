package no.nav.helse.behandling

import arrow.core.Either
import no.nav.helse.Behandlingsfeil
import no.nav.helse.behandling.mvp.*
import no.nav.helse.behandling.søknad.Fraværstype
import no.nav.helse.behandling.søknad.Sykepengesøknad

fun FaktagrunnlagResultat.mvpFilter(): Either<Behandlingsfeil, FaktagrunnlagResultat> {
    val mvpKriterier = listOf(
            sjekkSvarISøknaden(originalSøknad),
            vurderMVPKriterierForMedlemskap(faktagrunnlag.tps),
            vurderMVPKriterierForOpptjeningstid(faktagrunnlag.arbeidInntektYtelse.arbeidsforhold),
            vurderMVPKriterierForArbeidsforhold(originalSøknad.arbeidsgiver, faktagrunnlag.arbeidInntektYtelse.arbeidsforhold),
            vurderMVPKriterierForSykepengegrunnlaget(originalSøknad.startSyketilfelle, originalSøknad.soknadsperioder, faktagrunnlag.beregningsperiode),
            vurderMVPKriterierForAndreYtelser(faktagrunnlag.arbeidInntektYtelse.ytelser)
    )

    return if (mvpKriterier.any { it != null }) {
        Either.Left(Behandlingsfeil.mvpFilter(originalSøknad.id, originalSøknad.type, mvpKriterier.filterNotNull()))
    } else {
        Either.Right(this)
    }
}

private fun sjekkSvarISøknaden(søknad: Sykepengesøknad): MVPFeil? {
    return with (søknad) {
        when {
            andreInntektskilder.isNotEmpty() -> MVPFeil("Andre inntekter i søknaden", "Søker har svart han/hun har andre inntekter")
            fravær.any { it.type == Fraværstype.PERMISJON } -> MVPFeil("Har permisjon", "Søker har svart han/hun har permisjon")
            fravær.any { it.type == Fraværstype.UTDANNING_DELTID } -> MVPFeil("Har deltidstudier", "Søker har opplyst at han/hun har studier (deltid)")
            fravær.any { it.type == Fraværstype.UTDANNING_FULLTID } -> MVPFeil("Har fulltidstudier", "Søker har opplyst at han/hun har studier (fulltid)")
            fravær.any { it.type == Fraværstype.UTLANDSOPPHOLD } -> MVPFeil("Har utenlandsopphold", "Søker har opplyst at han/hun har utenlandsopphold")
            else -> null
        }
    }
}
