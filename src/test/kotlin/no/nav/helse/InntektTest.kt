package no.nav.helse

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class InntektTest {

    private val LOG = LoggerFactory.getLogger(InntektTest::class.java.name)

    @Test
    fun testParsingAvInntektsJsonRespons() {
        val inntektsdata = lastFil("inntekt_sparkelsvar_2.json")
        assertEquals(12, inntektsdata.arbeidsInntektIdentListe[0].arbeidsInntektMaaned.size)
    }

    private fun lastFil(ressursnavn : String) : InntektsOppslagResultat {
        val mapper = jacksonObjectMapper().registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        return mapper.readValue(InntektTest::class.java.classLoader.getResourceAsStream(ressursnavn))
    }
}