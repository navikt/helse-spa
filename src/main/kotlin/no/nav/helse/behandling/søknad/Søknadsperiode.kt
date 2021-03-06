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

@JsonSerialize(using = SøknadsperiodeSerializer::class)
@JsonDeserialize(using = SøknadsperiodeDeserializer::class)
data class Søknadsperiode(val jsonNode: JsonNode) {
    val fom = LocalDate.parse(jsonNode.get("fom").textValue())
    val tom = LocalDate.parse(jsonNode.get("tom").textValue())
    val sykmeldingsgrad get() = jsonNode.get("sykmeldingsgrad").asInt()
    val faktiskGrad
        get() = jsonNode.get("faktiskGrad")?.let {
            if (it.isNull) {
                null
            } else {
                it.asInt()
            }
        }

    val avtaltTimer
        get() = jsonNode.get("avtaltTimer")?.let {
            if (it.isNull) {
                null
            } else {
                it.asInt()
            }
        }
    val faktiskTimer
        get() = jsonNode.get("faktiskTimer")?.let {
            if (it.isNull) {
                null
            } else {
                it.asInt()
            }
        }
}

class SøknadsperiodeSerializer : StdSerializer<Søknadsperiode>(Søknadsperiode::class.java) {
    override fun serialize(søknad: Søknadsperiode?, gen: JsonGenerator?, provider: SerializerProvider?) {
        gen?.writeObject(søknad?.jsonNode)
    }
}

class SøknadsperiodeDeserializer : StdDeserializer<Søknadsperiode>(Søknadsperiode::class.java) {
    override fun deserialize(parser: JsonParser?, context: DeserializationContext?) =
        Søknadsperiode(defaultObjectMapper.readTree(parser))
}
