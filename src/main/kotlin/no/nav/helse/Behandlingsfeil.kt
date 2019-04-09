package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import no.nav.helse.behandling.Sykepengesøknad
import no.nav.helse.behandling.UavklarteFakta
import no.nav.helse.behandling.Vilkårsprøving

interface Behandlingsfeil {
    val feilmelding: String

    data class Deserialiseringsfeil(val soknadId: String, val json: JsonNode, override val feilmelding: String): Behandlingsfeil

    data class RegisterFeil(override val feilmelding: String, val søknad: Sykepengesøknad): Behandlingsfeil

    data class Avklaringsfeil(val uavklarteFakta: UavklarteFakta, override val feilmelding: String): Behandlingsfeil

    data class Vilkårsprøvingsfeil(val vilkårsprøving: Vilkårsprøving, override val feilmelding: String): Behandlingsfeil

    data class Beregningsfeil(val vilkårsprøving: Vilkårsprøving, override val feilmelding: String): Behandlingsfeil


    companion object {

        // deserializering feilet pga null-verdi som ikke kan være null
        fun manglendeFeilDeserialiseringsfeil(soknadId: String, json: JsonNode, exception: MissingKotlinParameterException) = Deserialiseringsfeil(soknadId, json, "Det mangler felt ${exception.parameter} i søknad med id $soknadId.")

        // deserializering feilet av ukjent årsak
        fun ukjentDeserialiseringsfeil(soknadId: String, json: JsonNode, exception: Exception) = Deserialiseringsfeil(soknadId, json, "Det er en ukjent feil i søknaden (id $soknadId) som gjør at vi ikke kan tolke den: ${exception.javaClass.simpleName} : ${exception.message}")

        // vi klarte ikke avklare alle fakta
        fun avklaringsfeil(uavklarteFakta: UavklarteFakta) = Avklaringsfeil(uavklarteFakta, "Kunne ikke fastsette alle fakta.")

        // vi klarte ikke vilkårsprøve, eller vilkårsprøving feilet
        fun vilkårErIkkeOppfylt(vilkårsprøving: Vilkårsprøving) = Vilkårsprøvingsfeil(vilkårsprøving, "Vilkår er ikke oppfylt.")

        // her feilet noe under _beregning, men vi har ikke del-resultat, bare exception
        fun beregningsfeil(vilkårsprøving: Vilkårsprøving, exception: Exception) = Beregningsfeil(vilkårsprøving, "Beregning feilet: ${exception.javaClass.simpleName}: ${exception.message}.")

        fun registerFeil(exception: Exception, søknad: Sykepengesøknad):RegisterFeil = RegisterFeil("Feil i opphenting av register-data: ${exception.javaClass.simpleName} : ${exception.message}\"", søknad)

    }
}
