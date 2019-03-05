package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import no.nav.helse.behandling.UavklarteFakta
import no.nav.helse.behandling.Vilkårsprøving

class Behandlingsfeil {

    companion object {

        // TODO: Do something useful with this

        // deserializering feilet pga null-verdi som ikke kan være null
        fun from(soknad: JsonNode, exception: MissingKotlinParameterException) = Behandlingsfeil()

        // deserializering feilet av ukjent årsak
        fun from(soknad: JsonNode, exception: Exception) = Behandlingsfeil()

        // vi klarte ikke avklare alle fakta
        fun from(uavklarteFakta: UavklarteFakta) = Behandlingsfeil()

        // vi klarte ikke vilkårsprøve, eller vilkårsprøving feilet
        fun from(vilkårsprøving: Vilkårsprøving) = Behandlingsfeil()

        // her feilet noe under _beregning, men vi har ikke del-resultat, bare exception
        fun from(vilkårsprøving: Vilkårsprøving, e: Exception) = Behandlingsfeil()

    }
}