package no.nav.helse.fastsetting

import no.nav.helse.Tpsfakta
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TpsfaktaTest {

    @Test
    fun `alder er riktig når bursdag er i dag`() {
        val iFjor = LocalDate.now().minusYears(1)
        val fakta = Tpsfakta(iFjor, "")

        assertEquals(1, fakta.alder())
    }

    @Test
    fun `alder er riktig når bursdag var i går`() {
        val iFjor = LocalDate.now().minusYears(1).minusDays(1)
        val fakta = Tpsfakta(iFjor, "")

        assertEquals(1, fakta.alder())
    }

    @Test
    fun `alder er riktig når bursdag er i morgen`() {
        val iFjor = LocalDate.now().minusYears(1).plusDays(1)
        val fakta = Tpsfakta(iFjor, "")

        assertEquals(0, fakta.alder())
    }
}
