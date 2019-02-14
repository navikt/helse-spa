package no.nav.helse

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class SykepengesoknadTest {

    private val LOG = LoggerFactory.getLogger(SykepengesoknadTest::class.java.name)

    @Test
    fun jsonTester() {
        val mapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        val sykepengesoknad : Sykepengesoknad = mapper.readValue(
                SykepengesoknadTest::class.java.classLoader.getResourceAsStream("enkel_soknad.json"))
        LOG.info(sykepengesoknad.aktorId)
        assertNotNull(sykepengesoknad.fom.toString())
    }
}