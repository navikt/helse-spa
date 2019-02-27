package no.nav.helse.serde

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer

val jsonNodeSerde: Serde<JsonNode> = Serdes.serdeFrom(JacksonSerializer(), JacksonNodeDeserializer())


val defaultObjectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

class JacksonSerializer<T> : Serializer<T> {
    override fun serialize(topic: String?, data: T?): ByteArray {
        return when (data) {
            null -> ByteArray(0)
            else -> defaultObjectMapper.writeValueAsBytes(data)
        }
    }

    override fun close() {}
    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
}

class JacksonDeserializer<T>(private val type: Class<T>) : Deserializer<T> {
    override fun deserialize(topic: String?, data: ByteArray?): T? {
        return when (data) {
            null -> null
            else -> defaultObjectMapper.readValue(data, type)
        }
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}

class JacksonNodeDeserializer() : Deserializer<JsonNode> {
    override fun deserialize(topic: String?, data: ByteArray?): JsonNode? {
        return when (data) {
            null -> null
            else -> defaultObjectMapper.readTree(data)
        }
    }

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {}
    override fun close() {}
}