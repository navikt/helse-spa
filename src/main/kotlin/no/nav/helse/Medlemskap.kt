package no.nav.helse

import no.nav.helse.sykepenger.vilkar.inngangsvilkar.søkerBorINorge
import no.nav.nare.core.specifications.Spesifikasjon

val erMedlem = Spesifikasjon<Sykepengesøknad>(
        beskrivelse = "",
        identitet = ""
) { søknad -> søkerBorINorge(søknad.faktagrunnlag.tps.bostedland) }