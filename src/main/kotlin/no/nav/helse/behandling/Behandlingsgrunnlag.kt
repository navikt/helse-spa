package no.nav.helse.behandling

import no.nav.helse.behandling.søknad.Sykepengesøknad
import no.nav.nare.core.evaluations.Evaluering

data class Behandlingsgrunnlag(
        val originalSøknad: Sykepengesøknad,
        val faktagrunnlag: Faktagrunnlag,
        val avklarteVerdier: AvklarteVerdier,
        val vilkårsprøving: Evaluering
)
