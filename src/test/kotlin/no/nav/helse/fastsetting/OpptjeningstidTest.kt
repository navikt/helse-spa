package no.nav.helse.fastsetting

import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsforholdDTO
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsgiverDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpptjeningstidTest {

    @Test
    fun `kan ikke fastsette opptjeningstid om arbeidsforholdet er uavklart`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val arbeidsforhold = Vurdering.Uavklart<ArbeidsforholdDTO, List<ArbeidsforholdDTO>>(Vurdering.Uavklart.Årsak.HAR_IKKE_DATA, "UKJENT", "", emptyList())
        val actual = vurderOpptjeningstid(Opptjeningsgrunnlag(førsteSykdomsdag, arbeidsforhold))

        when (actual) {
            is Vurdering.Uavklart -> {
                assertEquals("Kan ikke avklare opptjeningstid når arbeidsforholdet ikke er avklart", actual.begrunnelse)
                assertEquals(Vurdering.Uavklart.Årsak.HAR_IKKE_DATA, actual.årsak)
            }
            is Vurdering.Avklart -> fail { "Expected Vurdering.Uavklart to be returned" }
        }
    }

    @Test
    fun `opptjeningstid er lik antall dager søker har vært i arbeid før han eller hun ble syk`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val arbeidsforhold = listOf(
                ArbeidsforholdDTO("Arbeidstaker", ArbeidsgiverDTO("11223344", "Organisasjon"), LocalDate.parse("2018-12-01"), null)
        )
        val arbeidsforholdVurdering = Vurdering.Avklart(arbeidsforhold[0], "", arbeidsforhold, "SPA")

        val actual = vurderOpptjeningstid(Opptjeningsgrunnlag(førsteSykdomsdag, arbeidsforholdVurdering))

        when (actual) {
            is Vurdering.Avklart -> {
                assertEquals("Søker er i et aktivt arbeidsforhold", actual.begrunnelse)
                assertEquals(31, actual.fastsattVerdi)
            }
            is Vurdering.Uavklart -> fail { "Expected Vurdering.Avklart to be returned" }
        }
    }
}
