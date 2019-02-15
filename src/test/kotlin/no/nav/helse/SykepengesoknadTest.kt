package no.nav.helse

import no.nav.helse.serde.SykepengesoknadDeserializer
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class SykepengesoknadTest {

    private val LOG = LoggerFactory.getLogger(SykepengesoknadTest::class.java.name)

    @Test
    fun jsonTester() {
        val sykepengesoknad: Sykepengesoknad = SykepengesoknadDeserializer.deserialize(null, SykepengesoknadTest::class.java.classLoader.getResourceAsStream("enkel_soknad.json").readBytes())!!
        LOG.info(sykepengesoknad.aktorId)
        assertNotNull(sykepengesoknad.fom.toString())
    }
}