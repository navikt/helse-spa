package no.nav.helse.behandling

import no.nav.helse.Behandlingsfeil
import no.nav.helse.Either

fun vedtak(beregning: Sykepengeberegning): Either<Behandlingsfeil, SykepengeVedtak> =
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