package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import no.nav.helse.behandling.UavklarteFakta
import no.nav.helse.behandling.Vilkårsprøving

sealed class Behandlingsfeil {

    data class Deserialiseringsfeil(val json: JsonNode, val exception: java.lang.Exception, val feilmelding: String): Behandlingsfeil()

    data class Avklaringsfeil(val uavklarteFakta: UavklarteFakta, val feilmelding: String): Behandlingsfeil()

    data class Vilkårsprøvingsfeil(val vilkårsprøving: Vilkårsprøving, val feilmelding: String): Behandlingsfeil()

    data class Beregningsfeil(val vilkårsprøving: Vilkårsprøving, val exception: java.lang.Exception, val feilmelding: String): Behandlingsfeil()

    companion object {

        // deserializering feilet pga null-verdi som ikke kan være null
        fun manglendeFeilDeserialiseringsfeil(json: JsonNode, exception: MissingKotlinParameterException) = Deserialiseringsfeil(json, exception, "Det mangler felt i søknaden.")

        // deserializering feilet av ukjent årsak
        fun ukjentDeserialiseringsfeil(json: JsonNode, exception: Exception) = Deserialiseringsfeil(json, exception, "Det er en ukjent feil i søknaden som gjør at vi ikke kan tolke den.")

        // vi klarte ikke avklare alle fakta
        fun avklaringsfeil(uavklarteFakta: UavklarteFakta) = Avklaringsfeil(uavklarteFakta, "Kunne ikke fastsette alle fakta.")

        // vi klarte ikke vilkårsprøve, eller vilkårsprøving feilet
        fun vilkårErIkkeOppfylt(vilkårsprøving: Vilkårsprøving) = Vilkårsprøvingsfeil(vilkårsprøving, "Vilkår er ikke oppfylt.")

        // her feilet noe under _beregning, men vi har ikke del-resultat, bare exception
        fun beregningsfeil(vilkårsprøving: Vilkårsprøving, e: Exception) = Beregningsfeil(vilkårsprøving, e, "Beregning feilet.")

    }
}