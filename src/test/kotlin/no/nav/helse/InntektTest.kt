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
        val mapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        val inntektsfakta : Inntektsfakta = mapper.readValue(
                InntektTest::class.java.classLoader.getResourceAsStream("inntekt_sparkelsvar_1.json"))
        LOG.info(inntektsfakta.arbeidsInntektIdentListe.toString())
        assertEquals(12, inntektsfakta.arbeidsInntektIdentListe[0].arbeidsInntektMaaned.size)
        val startMåned = 2
        for (i in 0..11) {
            assertEquals(
                    if (startMåned + i > 12) startMåned + i - 12 else startMåned + i,
                    inntektsfakta.arbeidsInntektIdentListe[0].arbeidsInntektMaaned[i].aarMaaned.month.value)
            // faktisk ikke tilfelle i testfila per nå:
            /*assertEquals(1,
                    inntektsFakta.arbeidsInntektIdentListe[0].arbeidsInntektMaaned[i].arbeidsInntektInformasjon.inntektListe.size)*/
            assertEquals(if (i <= 2) 50000L else 60000L,
                    inntektsfakta.arbeidsInntektIdentListe[0].arbeidsInntektMaaned[i].arbeidsInntektInformasjon.inntektListe[0].beloep)
        }
    }

    @Test
    fun test1() {
        val d = LocalDate.now()
        println(d.format(DateTimeFormatter.ofPattern("yyyy-MM")))
    }
}