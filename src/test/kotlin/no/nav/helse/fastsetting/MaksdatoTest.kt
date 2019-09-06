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
    fun `Søker med avklart alder skal få maksdato i fremtiden avklart`() {

        val alderVurdering = Vurdering.Avklart(39, begrunnelse_p_8_51, Aldersgrunnlag(LocalDate.of(1978, 5, 14)), "SPA")
        val fom = LocalDate.of(2019, 6, 14)
        val startSyketilfelle = LocalDate.of(2019, 6, 14)
        val vurdering = vurderMaksdato(alderVurdering,
                startSyketilfelle, fom,
                Yrkesstatus.ARBEIDSTAKER,
                sykepengehistorikkDerSykepengedagerIkkeErOppbrukt())

        when (vurdering) {
            is Vurdering.Uavklart -> fail("Vurdering skal være avklart, maksdato skal være 2020-04-13")
            is Vurdering.Avklart -> assertThat(vurdering.fastsattVerdi).isEqualTo(LocalDate.of(2020, 4, 13))
        }
    }

    @Test
    fun `Tidligere sykepengeperioder skal ikke inneholde duplikater`() {

        val alderVurdering = Vurdering.Avklart(39, begrunnelse_p_8_51, Aldersgrunnlag(LocalDate.of(1978, 5, 14)), "SPA")
        val fom = LocalDate.of(2019, 6, 14)
        val startSyketilfelle = LocalDate.of(2019, 6, 14)
        val vurdering = vurderMaksdato(alderVurdering,
                startSyketilfelle, fom,
                Yrkesstatus.ARBEIDSTAKER,
                sykepengehistorikkMedDuplikater())

        when (vurdering) {
            is Vurdering.Uavklart -> fail("Vurdering skal være avklart")
            is Vurdering.Avklart -> assertThat(vurdering.grunnlag?.tidligerePerioder).isEqualTo(sykepengehistorikkUtenDuplikater())
        }
    }

    private fun sykepengehistorikkDerSykepengedagerIkkeErOppbrukt(): List<AnvistPeriodeDTO> =
            listOf(AnvistPeriodeDTO(of(2019, 2, 15), of(2019, 3, 11)),
                    AnvistPeriodeDTO(of(2019, 3, 12), of(2019, 3, 31)))

    private fun sykepengehistorikkMedDuplikater(): List<AnvistPeriodeDTO> =
            listOf(AnvistPeriodeDTO(of(2019, 2, 15), of(2019, 3, 11)),
                    AnvistPeriodeDTO(of(2019, 2, 15), of(2019, 3, 11)),
                    AnvistPeriodeDTO(of(2019, 3, 12), of(2019, 3, 31)),
                    AnvistPeriodeDTO(of(2019, 3, 12), of(2019, 3, 31)))

    private fun sykepengehistorikkUtenDuplikater(): List<Tidsperiode> =
            listOf(Tidsperiode(of(2019, 2, 15), of(2019, 3, 11)),
                    Tidsperiode(of(2019, 3, 12), of(2019, 3, 31)))


}