package no.nav.helse.behandling

import no.nav.helse.behandling.søknad.Sykepengesøknad

data class FaktagrunnlagResultat(
        val originalSøknad: Sykepengesøknad,
        val faktagrunnlag: Faktagrunnlag
)
