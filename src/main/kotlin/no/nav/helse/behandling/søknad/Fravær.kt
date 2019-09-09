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
import java.time.LocalDate

@JsonSerialize(using = Fravær.FraværSerializer::class)
@JsonDeserialize(using = Fravær.FraværDeserializer::class)
data class Fravær(val jsonNode: JsonNode) {

    val fom = with(jsonNode.get("fom")) { LocalDate.parse(textValue())!! }
    val tom = with(jsonNode.get("tom")) { LocalDate.parse(textValue())!! }
    val type get() = Fraværstype.valueOf(jsonNode.get("type").textValue())

    class FraværSerializer : StdSerializer<Fravær>(Fravær::class.java) {
        override fun serialize(søknad: Fravær?, gen: JsonGenerator?, provider: SerializerProvider?) {
            gen?.writeObject(søknad?.jsonNode)
        }
    }

    class FraværDeserializer : StdDeserializer<Fravær>(Fravær::class.java) {
        override fun deserialize(parser: JsonParser?, context: DeserializationContext?) =
            Fravær(defaultObjectMapper.readTree(parser))
    }
}
