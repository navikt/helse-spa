package no.nav.helse.behandling

import no.nav.helse.Behandlingsfeil
import no.nav.helse.Either
import no.nav.helse.Either.*

fun Sykepengesøknad.mvpFilter(): Either<Behandlingsfeil, Sykepengesøknad> {
    return if (andreInntektskilder.isNotEmpty()) {
        Left(Behandlingsfeil.mvpFiler(id, MVPFilterType.ANDRE_INNTEKTER_I_SØKNADEN))
    } else if (!arbeidsgiverForskutterer){
        Left(Behandlingsfeil.mvpFiler(id, MVPFilterType.ARBEIDSGIVER_FORSKUTTERER_IKKE))
    } else {
        Right(this)
    }
}

enum class MVPFilterType {
    ANDRE_INNTEKTER_I_SØKNADEN,
    ARBEIDSGIVER_FORSKUTTERER_IKKE
}