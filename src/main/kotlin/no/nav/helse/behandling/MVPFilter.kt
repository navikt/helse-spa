package no.nav.helse.behandling

import no.nav.helse.Behandlingsfeil
import no.nav.helse.Either

fun Sykepengesøknad.mvpFilter(): Either<Behandlingsfeil, Sykepengesøknad> {
    return if (andreInntektskilder.isNotEmpty()) {
        Either.Left(Behandlingsfeil.mvpFiler(id, MVPFilterType.ANDRE_INNTEKTER_I_SØKNADEN))
    } else {
        Either.Right(this)
    }
}

enum class MVPFilterType {
    ANDRE_INNTEKTER_I_SØKNADEN
}