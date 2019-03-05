package no.nav.helse.behandling

import no.nav.helse.Behandlingsfeil
import no.nav.helse.Either
import no.nav.helse.flatMap

fun vedtak(eitherBeregning: Either<Behandlingsfeil, Sykepengeberegning>): Either<Behandlingsfeil, SykepengeVedtak> = eitherBeregning.flatMap { beregning ->
    Either.Right(
            SykepengeVedtak(
                    originalSøknad = beregning.originalSøknad,
                    faktagrunnlag = beregning.faktagrunnlag,
                    vilkårsprøving = beregning.vilkårsprøving,
                    avklarteVerdier = beregning.avklarteVerdier,
                    beregning = beregning.beregning,
                    vedtak = Vedtak("Burde antagelig gjøre noe med dette.")
            )
    )
}