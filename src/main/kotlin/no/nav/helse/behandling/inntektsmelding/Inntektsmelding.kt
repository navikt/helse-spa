package no.nav.helse.behandling.inntektsmelding

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
import java.time.LocalDate

@JsonSerialize(using = InntektsmeldingSerializer::class)
@JsonDeserialize(using = InntektsmeldingDeserializer::class)
data class Inntektsmelding(val jsonNode: JsonNode) {

    val id = jsonNode["id"].asText()!!
    val inntekt = jsonNode["inntekt"].asLong()!!
    val arbeidsgiverperioder: List<Periode> get() = jsonNode["arbeidsgiverperioder"].map { Periode(it) }
}

class InntektsmeldingSerializer : StdSerializer<Inntektsmelding>(Inntektsmelding::class.java) {
    override fun serialize(sykmelding: Inntektsmelding?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeObject(sykmelding?.jsonNode)
    }
}

class InntektsmeldingDeserializer : StdDeserializer<Inntektsmelding>(Inntektsmelding::class.java) {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    override fun deserialize(parser: JsonParser?, context: DeserializationContext?) =
        Inntektsmelding(objectMapper.readTree(parser))
}

@JsonSerialize(using = InntektsmeldingSerializer::class)
@JsonDeserialize(using = InntektsmeldingDeserializer::class)
data class Periode(val jsonNode: JsonNode) {
    val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
    val tom: LocalDate = LocalDate.parse(jsonNode["tom"].textValue())
}
