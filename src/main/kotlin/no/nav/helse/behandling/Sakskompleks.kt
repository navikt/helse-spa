package no.nav.helse.behandling

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behandling.inntektsmelding.Inntektsmelding
import no.nav.helse.behandling.sykmelding.SykmeldingMessage
import no.nav.helse.behandling.søknad.Sykepengesøknad
import no.nav.helse.serde.safelyUnwrapDate

@JsonSerialize(using = SakskompleksSerializer::class)
@JsonDeserialize(using = SakskompleksDeserializer::class)
data class Sakskompleks(val jsonNode: JsonNode) {
    val id = jsonNode["id"].asText()!!
    val aktørId = jsonNode["aktørId"].asText()!!
    val sykmeldinger get() = jsonNode["sykmeldinger"].map { SykmeldingMessage(it) }
    val søknader get() = jsonNode["søknader"].map { Sykepengesøknad(it) }
    val inntektsmeldinger get() = jsonNode["inntektsmeldinger"].map { Inntektsmelding(it) }
    val orgnummer get() = jsonNode["orgnummer"].asText()
    val startSyketilfelle get() = jsonNode["syketilfelleStartdato"].safelyUnwrapDate()!!
    val sluttSyketilfelle get() = jsonNode["syketilfelleSluttdato"].safelyUnwrapDate()!!
}

class SakskompleksSerializer : StdSerializer<Sakskompleks>(Sakskompleks::class.java) {
    override fun serialize(sakskompleks: Sakskompleks?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeObject(sakskompleks?.jsonNode)
    }
}

class SakskompleksDeserializer : StdDeserializer<Sakskompleks>(Sakskompleks::class.java) {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?) =
        Sakskompleks(objectMapper.readTree(parser))

}
