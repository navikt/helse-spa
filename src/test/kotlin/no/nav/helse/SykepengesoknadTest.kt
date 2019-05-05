package no.nav.helse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.dto.SykepengesøknadV2DTO
import no.nav.helse.streams.defaultObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SykepengesoknadTest {

    @Test
    fun jsonTester() {
        val sykepengesøknad: SykepengesøknadV2DTO = defaultObjectMapper.readValue(SykepengesoknadTest::class.java.classLoader.getResourceAsStream("søknader/arbeidstaker_sendt_nav.json"))
        assertEquals("1c6d9930-ce09-49dc-bcb3-b6fee2ff4e32", sykepengesøknad.id)
    }
}
