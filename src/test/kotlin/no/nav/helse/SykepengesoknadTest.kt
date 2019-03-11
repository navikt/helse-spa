package no.nav.helse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.behandling.SykepengesøknadV2DTO
import no.nav.helse.streams.defaultObjectMapper
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class SykepengesoknadTest {

    private val LOG = LoggerFactory.getLogger(SykepengesoknadTest::class.java.name)

    @Test
    fun jsonTester() {
        val sykepengesøknad: SykepengesøknadV2DTO = defaultObjectMapper.readValue(SykepengesoknadTest::class.java.classLoader.getResourceAsStream("enkel_soknad.json"))
        LOG.info(sykepengesøknad.aktorId)
        assertNotNull(sykepengesøknad.fom.toString())
    }
}
