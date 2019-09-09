package no.nav.helse.behandling.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.streams.defaultObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime

@JsonSerialize(using = SykepengesøknadSerializer::class)
@JsonDeserialize(using = SykepengesøknadDeserializer::class)
data class Sykepengesøknad(val jsonNode: JsonNode) {

    val version: Version

    val id get() = jsonNode.get("id").textValue()!!

    val aktorId get() = jsonNode.get("aktorId").textValue()!!

    val type
        get() = when (version) {
            is Version.Version1 -> jsonNode.get("soknadstype").textValue()!!
            is Version.Version2 -> jsonNode.get("type").textValue()!!
        }

    val status
        get() = when {
            jsonNode.has("status") -> jsonNode.get("status").textValue()!!
            else -> "UKJENT"
        }

    val arbeidsgiver
        get() = with(jsonNode.path("arbeidsgiver")) {
            ArbeidsgiverFraSøknad(
                navn = get("navn").textValue(),
                orgnummer = get("orgnummer").textValue()
            )
        }

    val soktUtenlandsopphold get() = jsonNode.get("soktUtenlandsopphold").booleanValue()

    val fom
        get() = with(jsonNode.get("fom")) {
            LocalDate.parse(textValue())!!
        }

    val tom
        get() = with(jsonNode.get("tom")) {
            LocalDate.parse(textValue())!!
        }

    val startSyketilfelle
        get() = with(jsonNode.get("startSyketilfelle")) {
            LocalDate.parse(textValue())!!
        }

    val sendtNav
        get() = jsonNode.get("sendtNav").textValue()?.let {
            LocalDateTime.parse(it)
        }

    val sendtTilNAV
        get() = when {
            status != "SENDT" -> false
            jsonNode.has("sendtNav") -> !jsonNode.get("sendtNav").isNull
            else -> true
        }

    val soknadsperioder
        get() = with(jsonNode.get("soknadsperioder")) { map { Søknadsperiode(it) } }

    val fravær
        get() = with(jsonNode.get("fravar")) {
            map { Fravær(it) }
        }

    val andreInntektskilder
        get() = with(jsonNode.get("andreInntektskilder")) { map { Inntektskilde(it) } }

    val arbeidGjenopptatt
        get() = jsonNode.get("arbeidGjenopptatt")?.let { arbeidGjenopptattNode ->
            if (arbeidGjenopptattNode.isNull) {
                null
            } else {
                LocalDate.parse(arbeidGjenopptattNode.textValue())
            }
        }

    val harKorrigertArbeidstid
        get() = soknadsperioder.any { sp -> sp.avtaltTimer != null || sp.faktiskGrad != null || sp.faktiskTimer != null }

    init {
        version = when {
            jsonNode.has("soknadstype") -> Version.Version1
            jsonNode.has("type") -> Version.Version2
            else -> throw IllegalArgumentException(
                "was expecting a sykepengesøknad: ${defaultObjectMapper.writeValueAsString(
                    jsonNode
                )}"
            )
        }
    }

    sealed class Version {
        object Version1 : Version()
        object Version2 : Version()
    }
}
