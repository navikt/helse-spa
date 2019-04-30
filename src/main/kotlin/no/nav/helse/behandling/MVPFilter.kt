package no.nav.helse.behandling

import no.nav.helse.Behandlingsfeil
import no.nav.helse.Either
import no.nav.helse.Either.*

fun Sykepengesøknad.mvpFilter(): Either<Behandlingsfeil, Sykepengesøknad> =
        when {
            andreInntektskilder.isNotEmpty() -> Left(Behandlingsfeil.mvpFiler(id, MVPFilterType.ANDRE_INNTEKTER_I_SOKNADEN))
            !arbeidsgiverForskutterer -> Left(Behandlingsfeil.mvpFiler(id, MVPFilterType.ARBEIDSGIVER_FORSKUTTERER_IKKE))
            fravær.any { it.type == Fravarstype.PERMISJON } -> Left(Behandlingsfeil.mvpFiler(id, MVPFilterType.SOKER_HAR_PERMISJON))
            fravær.any { it.type == Fravarstype.UTDANNING_DELTID } -> Left(Behandlingsfeil.mvpFiler(id, MVPFilterType.SOKER_HAR_STUDIER))
            fravær.any { it.type == Fravarstype.UTDANNING_FULLTID } -> Left(Behandlingsfeil.mvpFiler(id, MVPFilterType.SOKER_HAR_STUDIER))
            fravær.any { it.type == Fravarstype.UTLANDSOPPHOLD } -> Left(Behandlingsfeil.mvpFiler(id, MVPFilterType.SOKER_HAR_UTENLANDSOPPHOLD))
            else -> Right(this)
        }

enum class MVPFilterType {
    ANDRE_INNTEKTER_I_SOKNADEN,
    ARBEIDSGIVER_FORSKUTTERER_IKKE,
    SOKER_HAR_PERMISJON,
    SOKER_HAR_STUDIER,
    SOKER_HAR_UTENLANDSOPPHOLD
}