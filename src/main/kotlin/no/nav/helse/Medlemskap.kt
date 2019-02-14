package no.nav.helse

import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.specifications.Spesifikasjon

val erMedlem = Spesifikasjon<BeriketSykepengesoknad>(
        beskrivelse = "",
        identitet = ""
) { søknad -> søkerBorI("Norge", søknad.faktagrunnlag.tps) }

fun søkerBorI(land: String, vurdering: Vurdering<Tpsfakta, *>): Evaluering =
        when (vurdering) {
            is Uavklart -> Evaluering.kanskje("Bostedsland er uavklart")
            is Avklart -> if (vurdering.fastsattVerdi.bostedland == land)
                Evaluering.ja("Søker er bosatt i $land")
            else
                Evaluering.nei("Søker er ikke bosatt i $land")
        }