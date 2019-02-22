package no.nav.helse

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InntektsFastsettingTest {

    val orgnummer = "974652269"

    @Test
    fun `gjennomsnittet av de tre siste kalendermånedene før arbeidsuførhet skal legges til grunn`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val inntekter = listOf(
                Inntekt(LocalDate.parse("2018-12-01").yearMonth(), 1, orgnummer),
                Inntekt(LocalDate.parse("2018-11-01").yearMonth(), 21, orgnummer),
                Inntekt(LocalDate.parse("2018-10-01").yearMonth(), 29, orgnummer),
                Inntekt(LocalDate.parse("2018-09-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2018-08-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2018-07-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2018-06-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2018-05-01").yearMonth(), 30, orgnummer)
        )
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, inntekter)

        Assertions.assertEquals(17, (fastsattSykepengegrunnlag as Vurdering.Avklart).fastsattVerdi.aktuellMånedsinntekt)
    }

    @Test
    fun `gjennomsnittet av kortere periode skal legges til grunn`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val inntekter = listOf(
                Inntekt(LocalDate.parse("2018-12-01").yearMonth(), 1, orgnummer),
                Inntekt(LocalDate.parse("2018-11-01").yearMonth(), 21, orgnummer)
        )
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, inntekter)

        Assertions.assertEquals(11, (fastsattSykepengegrunnlag as Vurdering.Avklart).fastsattVerdi.aktuellMånedsinntekt)
    }

    @Test
    fun `uavklart sykepengegrunnlag når det ikke er noen inntekter i beregningsperioden`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val inntekter = emptyList<Inntekt>()
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, inntekter)

        Assertions.assertEquals(Vurdering.Uavklart::class, fastsattSykepengegrunnlag::class)
        Assertions.assertEquals("Kan ikke avklare sykepengegrunnlaget fordi det ikke er inntekter i beregningsperioden", (fastsattSykepengegrunnlag as Vurdering.Uavklart).begrunnelse)
    }

    @Test
    fun `uavklart sykepengegrunnlag når det er flere enn tre inntekter i beregningsperioden`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val inntekter = listOf(
                Inntekt(LocalDate.parse("2018-12-01").yearMonth(), 1, orgnummer),
                Inntekt(LocalDate.parse("2018-12-01").yearMonth(), 10, orgnummer),
                Inntekt(LocalDate.parse("2018-11-01").yearMonth(), 21, orgnummer),
                Inntekt(LocalDate.parse("2018-11-01").yearMonth(), 31, orgnummer)
        )
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, inntekter)

        Assertions.assertEquals(Vurdering.Uavklart::class, fastsattSykepengegrunnlag::class)
        Assertions.assertEquals("Kan ikke avklare sykepengegrunnlaget fordi det er 4 inntekter i beregningsperioden, vi forventer tre eller færre.", (fastsattSykepengegrunnlag as Vurdering.Uavklart).begrunnelse)
    }

}