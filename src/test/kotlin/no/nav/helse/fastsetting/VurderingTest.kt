package no.nav.helse.fastsetting

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VurderingTest {

    @Test
    fun `avklart should have verdi`() {
        val vurdering: Vurdering<Int, Inntektfakta> = Vurdering.Avklart(5000, "", Inntektfakta(5000), "")

        assertTrue(vurdering is Vurdering.Avklart<Int, Inntektfakta>)
    }

    @Test
    fun `uavklart should have verdi`() {
        val vurdering: Vurdering<Int, Inntektfakta> = Vurdering.Uavklart(Vurdering.Uavklart.Årsak.KREVER_SKJØNNSMESSIG_VURDERING, "", "Inntekt må fastsettes ved skjønn", Inntektfakta(5000))

        assertTrue(vurdering is Vurdering.Uavklart<Int,Inntektfakta>)
    }
}

private class Inntektfakta(val beløp: Int)
