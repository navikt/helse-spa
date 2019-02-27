package no.nav.helse.fastsetting

import no.nav.helse.ArbeidsgiverFakta
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpptjeningstidTest {

    @Test
    fun `kan ikke fastsette opptjeningstid om søker ikke har noen arbeidsgivere`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val arbeidsforhold = emptyList<ArbeidsgiverFakta>()
        val actual = vurderOpptjeningstid(Opptjeningsgrunnlag(førsteSykdomsdag, arbeidsforhold))

        when (actual) {
            is Vurdering.Uavklart -> {
                assertEquals("Søker har 0 arbeidsforhold og vi forventer kun 1", actual.begrunnelse)
                assertEquals(Vurdering.Uavklart.Årsak.FALLER_UTENFOR_MVP, actual.årsak)
            }
            is Vurdering.Avklart -> fail { "Expected Vurdering.Uavklart to be returned" }
        }
    }

    @Test
    fun `kan ikke fastsette opptjeningstid om søker har flere enn 1 arbeidsgiver`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val arbeidsforhold = listOf(
                ArbeidsgiverFakta("11223344", null, LocalDate.now(), null),
                ArbeidsgiverFakta("11223344", null, LocalDate.now(), null)
        )
        val actual = vurderOpptjeningstid(Opptjeningsgrunnlag(førsteSykdomsdag, arbeidsforhold))

        when (actual) {
            is Vurdering.Uavklart -> {
                assertEquals("Søker har 2 arbeidsforhold og vi forventer kun 1", actual.begrunnelse)
                assertEquals(Vurdering.Uavklart.Årsak.FALLER_UTENFOR_MVP, actual.årsak)
            }
            is Vurdering.Avklart -> fail { "Expected Vurdering.Uavklart to be returned" }
        }
    }

    @Test
    fun `kan ikke fastsette opptjeningstid om søker har avsluttet arbeidsforholdet`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val arbeidsforhold = listOf(
                ArbeidsgiverFakta("11223344", null, LocalDate.now(), LocalDate.now())
        )
        val actual = vurderOpptjeningstid(Opptjeningsgrunnlag(førsteSykdomsdag, arbeidsforhold))

        when (actual) {
            is Vurdering.Uavklart -> {
                assertEquals("Søker har ett arbeidsforhold som han eller hun har avsluttet", actual.begrunnelse)
                assertEquals(Vurdering.Uavklart.Årsak.FALLER_UTENFOR_MVP, actual.årsak)
            }
            is Vurdering.Avklart -> fail { "Expected Vurdering.Uavklart to be returned" }
        }
    }

    @Test
    fun `opptjeningstid er lik antall dager søker har vært i arbeid før han eller hun ble syk`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val arbeidsforhold = listOf(
                ArbeidsgiverFakta("11223344", null, LocalDate.parse("2018-12-01"), null)
        )
        val actual = vurderOpptjeningstid(Opptjeningsgrunnlag(førsteSykdomsdag, arbeidsforhold))

        when (actual) {
            is Vurdering.Avklart -> {
                assertEquals("Søker er i et aktivt arbeidsforhold", actual.begrunnelse)
                assertEquals(31, actual.fastsattVerdi)
            }
            is Vurdering.Uavklart -> fail { "Expected Vurdering.Avklart to be returned" }
        }
    }
}
