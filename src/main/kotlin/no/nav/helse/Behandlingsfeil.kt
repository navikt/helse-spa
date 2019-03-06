package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.prometheus.client.Counter
import no.nav.helse.behandling.UavklarteFakta
import no.nav.helse.behandling.Vilkårsprøving
import no.nav.helse.fastsetting.Vurdering

sealed class Behandlingsfeil {
    data class Deserialiseringsfeil(val json: JsonNode, val feilmelding: String): Behandlingsfeil()

    data class RegisterFeil(val feilmelding: String): Behandlingsfeil()

    data class Avklaringsfeil(val uavklarteFakta: UavklarteFakta, val feilmelding: String): Behandlingsfeil() {
        fun tellUavklarte(avklaringsfeilCounter: Counter) {
            uavklarteFakta.uavklarteVerdier.asNamedList().forEach { (name, fakta) ->
                if (fakta is Vurdering.Uavklart) avklaringsfeilCounter.labels(name).inc()
            }
        }
    }

    data class Vilkårsprøvingsfeil(val vilkårsprøving: Vilkårsprøving, val feilmelding: String): Behandlingsfeil()

    data class Beregningsfeil(val vilkårsprøving: Vilkårsprøving, val feilmelding: String): Behandlingsfeil()


    companion object {

        // deserializering feilet pga null-verdi som ikke kan være null
        fun manglendeFeilDeserialiseringsfeil(json: JsonNode, exception: MissingKotlinParameterException) = Deserialiseringsfeil(json, "Det mangler felt ${exception.parameter} i søknaden.")

        // deserializering feilet av ukjent årsak
        fun ukjentDeserialiseringsfeil(json: JsonNode, exception: Exception) = Deserialiseringsfeil(json, "Det er en ukjent feil i søknaden som gjør at vi ikke kan tolke den: ${exception.javaClass.simpleName} : ${exception.message}")

        // vi klarte ikke avklare alle fakta
        fun avklaringsfeil(uavklarteFakta: UavklarteFakta) = Avklaringsfeil(uavklarteFakta, "Kunne ikke fastsette alle fakta.")

        // vi klarte ikke vilkårsprøve, eller vilkårsprøving feilet
        fun vilkårErIkkeOppfylt(vilkårsprøving: Vilkårsprøving) = Vilkårsprøvingsfeil(vilkårsprøving, "Vilkår er ikke oppfylt.")

        // her feilet noe under _beregning, men vi har ikke del-resultat, bare exception
        fun beregningsfeil(vilkårsprøving: Vilkårsprøving, exception: Exception) = Beregningsfeil(vilkårsprøving, "Beregning feilet: ${exception.javaClass.simpleName}: ${exception.message}.")

        fun registerFeil(exception: Exception):RegisterFeil = RegisterFeil("Feil i opphenting av register-data: ${exception.javaClass.simpleName} : ${exception.message}\"")

    }
}