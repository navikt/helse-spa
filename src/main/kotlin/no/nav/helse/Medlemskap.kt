package no.nav.helse

import no.nav.helse.sykepenger.vilkar.inngangsvilkar.søkerBorINorge
import no.nav.nare.core.specifications.Spesifikasjon

val erMedlem = Spesifikasjon<Sykepengesoknad>(
        beskrivelse = "",
        identitet = ""
) { søknad -> søkerBorINorge(søknad.faktagrunnlag.tps.bostedland) }