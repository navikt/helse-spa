package no.nav.helse.behandling.søknad

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import no.nav.helse.streams.defaultObjectMapper

class SykepengesøknadDeserializer: StdDeserializer<Sykepengesøknad>(Sykepengesøknad::class.java) {
    override fun deserialize(parser: JsonParser?, context: DeserializationContext?) =
            Sykepengesøknad(defaultObjectMapper.readTree(parser))

}
