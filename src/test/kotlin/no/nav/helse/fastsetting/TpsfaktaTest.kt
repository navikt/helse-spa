package no.nav.helse.fastsetting

import no.nav.helse.behandling.Tpsfakta
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TpsfaktaTest {

    @Test
    fun `alder er riktig når bursdag er i dag`() {
        val iFjor = LocalDate.now().minusYears(1)
        val fakta = Tpsfakta(iFjor, "", "NOR", "BOSA", null)

        assertEquals(1, fakta.alder())
    }

    @Test
    fun `alder er riktig når bursdag var i går`() {
        val iFjor = LocalDate.now().minusYears(1).minusDays(1)
        val fakta = Tpsfakta(iFjor, "", "NOR", "BOSA", null)

        assertEquals(1, fakta.alder())
    }

    @Test
    fun `alder er riktig når bursdag er i morgen`() {
        val iFjor = LocalDate.now().minusYears(1).plusDays(1)
        val fakta = Tpsfakta(iFjor, "", "NOR", "BOSA", null)

        assertEquals(0, fakta.alder())
    }
}
