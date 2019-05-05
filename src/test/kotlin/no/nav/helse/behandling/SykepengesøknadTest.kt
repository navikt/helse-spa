package no.nav.helse.behandling

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.behandling.søknad.Sykepengesøknad
import no.nav.helse.streams.defaultObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SykepengesøknadTest {

    @Test
    fun `serialisering av en søknad skal serialisere jsonNode`() {
        val søknadJson = defaultObjectMapper.readTree(SykepengesøknadTest::class.java
                .classLoader.getResource("søknader/arbeidstaker_sendt_nav.json").readText())

        val søknad = Sykepengesøknad(søknadJson)

        assertEquals(søknadJson, defaultObjectMapper.valueToTree(søknad))
    }

    @Test
    fun `deserialisering av en søknad skal deserialisere jsonNode`() {
        val søknad = defaultObjectMapper.readValue<Sykepengesøknad>(SykepengesøknadTest::class.java
                .classLoader.getResource("søknader/arbeidstaker_sendt_nav.json").readText())

        assertEquals("1c6d9930-ce09-49dc-bcb3-b6fee2ff4e32", søknad.id)
    }
}
