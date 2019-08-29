package no.nav.helse.fastsetting

import no.nav.helse.*
import no.nav.helse.oppslag.AnvistPeriodeDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDate.of

class MaksdatoTest {

    @Test
    fun `Søker med tilgjengelige sykepengedager skal få maksdato i fremtiden avklart`() {

        val alderVurdering = Vurdering.Avklart(39, begrunnelse_p_8_51, Aldersgrunnlag(LocalDate.of(1978, 5, 14)), "SPA")
        val fom = LocalDate.of(2019, 6, 14)
        val startSyketilfelle = LocalDate.of(2019, 6, 14)
        val tom = LocalDate.of(2019, 6, 28)
        val vurdering = vurderMaksdato(alderVurdering,
                startSyketilfelle, fom, tom,
                Yrkesstatus.ARBEIDSTAKER,
                sykepengehistorikkDerSykepengedagerIkkeErOppbrukt())

        when (vurdering) {
            is Vurdering.Uavklart -> fail("Vurdering skal være avklart, maksdato skal være 2020-04-13")
            is Vurdering.Avklart -> assertThat(vurdering.fastsattVerdi).isEqualTo(LocalDate.of(2020, 4, 13))
        }
    }

    @Test
    fun `Søker uten tilgjengelige sykepengedager skal få maksdato uavklart`() {

        val alderVurdering = Vurdering.Avklart(39, begrunnelse_p_8_51, Aldersgrunnlag(LocalDate.of(1978, 5, 14)), "SPA")
        val fom = LocalDate.of(2019, 6, 14)
        val startSyketilfelle = LocalDate.of(2019, 6, 14)
        val tom = LocalDate.of(2019, 6, 28)
        val vurdering = vurderMaksdato(alderVurdering,
                startSyketilfelle, fom, tom,
                Yrkesstatus.ARBEIDSTAKER,
                sykepengehistorikkDerSykepengedagerErOppbrukt())

        when (vurdering) {
            is Vurdering.Avklart -> fail("Vurdering skal være uavklart")
            is Vurdering.Uavklart -> assertThat(vurdering.årsak).isEqualTo(Vurdering.Uavklart.Årsak.FALLER_UTENFOR_MVP)
        }
    }

    private fun sykepengehistorikkDerSykepengedagerErOppbrukt(): List<AnvistPeriodeDTO> =
            listOf(AnvistPeriodeDTO(of(2018, 5, 9), of(2018, 5, 14)),
                    AnvistPeriodeDTO(of(2018, 5, 15), of(2018, 6, 14)),
                    AnvistPeriodeDTO(of(2018, 6, 5), of(2018, 6, 25)),
                    AnvistPeriodeDTO(of(2018, 6, 26), of(2018, 7, 16)),
                    AnvistPeriodeDTO(of(2018, 7, 17), of(2018, 8, 6)),
                    AnvistPeriodeDTO(of(2018, 8, 7), of(2018, 8, 27)),
                    AnvistPeriodeDTO(of(2018, 8, 28), of(2018, 9, 20)),
                    AnvistPeriodeDTO(of(2018, 9, 21), of(2018, 10, 14)),
                    AnvistPeriodeDTO(of(2018, 10, 15), of(2018, 11, 7)),
                    AnvistPeriodeDTO(of(2018, 11, 8), of(2018, 12, 23)),
                    AnvistPeriodeDTO(of(2018, 12, 24), of(2019, 1, 14)),
                    AnvistPeriodeDTO(of(2019, 1, 15), of(2019, 2, 14)),
                    AnvistPeriodeDTO(of(2019, 2, 15), of(2019, 3, 11)),
                    AnvistPeriodeDTO(of(2019, 3, 12), of(2019, 3, 31)),
                    AnvistPeriodeDTO(of(2019, 4, 15), of(2019, 4, 26)),
                    AnvistPeriodeDTO(of(2019, 4, 27), of(2019, 5, 3)),
                    AnvistPeriodeDTO(of(2019, 5, 10), of(2019, 5, 14)))

    private fun sykepengehistorikkDerSykepengedagerIkkeErOppbrukt(): List<AnvistPeriodeDTO> =
            listOf(AnvistPeriodeDTO(of(2019, 2, 15), of(2019, 3, 11)),
                    AnvistPeriodeDTO(of(2019, 3, 12), of(2019, 3, 31)))

}