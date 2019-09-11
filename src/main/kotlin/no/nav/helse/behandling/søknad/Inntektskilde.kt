package no.nav.helse.behandling.søknad

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import no.nav.helse.streams.defaultObjectMapper

@JsonSerialize(using = InntektskildeSerializer::class)
@JsonDeserialize(using = InntektskildeDeserializer::class)
data class Inntektskilde(val jsonNode: JsonNode) {
    val type get() = jsonNode.get("type").textValue()
    val sykemeldt get() = jsonNode.get("sykmeldt").asBoolean()
}

class InntektskildeSerializer : StdSerializer<Inntektskilde>(Inntektskilde::class.java) {
    override fun serialize(søknad: Inntektskilde?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeObject(søknad?.jsonNode)
    }
}

class InntektskildeDeserializer : StdDeserializer<Inntektskilde>(Inntektskilde::class.java) {
    override fun deserialize(parser: JsonParser?, context: DeserializationContext?) =
        Inntektskilde(defaultObjectMapper.readTree(parser))
}
