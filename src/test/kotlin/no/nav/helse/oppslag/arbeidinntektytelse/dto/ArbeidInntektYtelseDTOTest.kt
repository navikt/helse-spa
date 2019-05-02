package no.nav.helse.oppslag.arbeidinntektytelse.dto

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.streams.defaultObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArbeidInntektYtelseDTOTest {

    @Test
    fun `test at json kan deserialiseres til dto`() {
        val arbeidInntektYtelse: ArbeidInntektYtelseDTO = defaultObjectMapper.readValue(ArbeidInntektYtelseDTOTest::class.java.
                classLoader.getResourceAsStream("arbeidInntektYtelse.json"))
        assertEquals("889640782", arbeidInntektYtelse.arbeidsforhold[0].arbeidsgiver.identifikator)
    }
}
