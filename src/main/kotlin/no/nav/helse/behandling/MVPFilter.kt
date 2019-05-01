package no.nav.helse.behandling

import no.nav.helse.Behandlingsfeil
import no.nav.helse.Either
import no.nav.helse.Either.Left
import no.nav.helse.Either.Right

fun Sykepengesøknad.mvpFilter(): Either<Behandlingsfeil, Sykepengesøknad> =
        when {
            andreInntektskilder.isNotEmpty() -> Left(Behandlingsfeil.mvpFiler(this, MVPFilterType.ANDRE_INNTEKTER_I_SOKNADEN))
            !arbeidsgiverForskutterer -> Left(Behandlingsfeil.mvpFiler(this, MVPFilterType.ARBEIDSGIVER_FORSKUTTERER_IKKE))
            fravær.any { it.type == Fraværstype.PERMISJON } -> Left(Behandlingsfeil.mvpFiler(this, MVPFilterType.SOKER_HAR_PERMISJON))
            fravær.any { it.type == Fraværstype.UTDANNING_DELTID } -> Left(Behandlingsfeil.mvpFiler(this, MVPFilterType.SOKER_HAR_STUDIER))
            fravær.any { it.type == Fraværstype.UTDANNING_FULLTID } -> Left(Behandlingsfeil.mvpFiler(this, MVPFilterType.SOKER_HAR_STUDIER))
            fravær.any { it.type == Fraværstype.UTLANDSOPPHOLD } -> Left(Behandlingsfeil.mvpFiler(this, MVPFilterType.SOKER_HAR_UTENLANDSOPPHOLD))
            else -> Right(this)
        }

enum class MVPFilterType {
    ANDRE_INNTEKTER_I_SOKNADEN,
    ARBEIDSGIVER_FORSKUTTERER_IKKE,
    SOKER_HAR_PERMISJON,
    SOKER_HAR_STUDIER,
    SOKER_HAR_UTENLANDSOPPHOLD
}
