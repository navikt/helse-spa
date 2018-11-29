package no.nav.helse.vilkaar

import no.nav.helse.Soknad
import no.nav.helse.vilkaar.regler.*
import no.nav.nare.specifications.Specification

fun harOpptjening(): Specification<Soknad> {
    return Yrkesaktiv()
            .or(MottarDagpenger())
            .or(MottarSykepenger())
            .or(MottarPleiepenger())
            .or(MottarForeldrepenger()
                    .and(GrunnlagForForeldrepengerErIkkeAAP()
                            .or(YrkesaktivEllerSidestiltYtelseForForeldrepenger())))
}
