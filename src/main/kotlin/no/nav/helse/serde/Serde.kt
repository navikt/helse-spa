package no.nav.helse.serde

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.Sykepengesoknad
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer

val sykepengesoknadSerde: Serde<Sykepengesoknad> = Serdes.serdeFrom(SykepengesoknadSerializer, SykepengesoknadDeserializer)

val defaultObjectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

object SykepengesoknadSerializer : Serializer<Sykepengesoknad> {
    override fun serialize(topic: String?, data: Sykepengesoknad?): ByteArray {
        return when (data) {
            null -> ByteArray(0)
            else -> defaultObjectMapper.writeValueAsBytes(data)
        }
    }

    override fun close() {}
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
}

object SykepengesoknadDeserializer : Deserializer<Sykepengesoknad> {
    override fun deserialize(topic: String?, data: ByteArray?): Sykepengesoknad? {
        return when (data) {
            null -> null
            else -> defaultObjectMapper.readValue(data, Sykepengesoknad::class.java)
        }
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}
