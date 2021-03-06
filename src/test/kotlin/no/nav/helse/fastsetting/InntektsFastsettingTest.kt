package no.nav.helse.fastsetting

import no.nav.helse.oppslag.Inntekt
import no.nav.helse.oppslag.Inntektsarbeidsgiver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class InntektsFastsettingTest {

    val orgnummer = "974652269"
    val annetOrgnummer = "985538859"

    @Test
    fun `gjennomsnittet av de tre siste kalendermånedene før arbeidsuførhet skal legges til grunn`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val inntekter = listOf(
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-12"), BigDecimal.valueOf(1), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-11"), BigDecimal.valueOf(21), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-10"), BigDecimal.valueOf(29), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-09"), BigDecimal.valueOf(30), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-08"), BigDecimal.valueOf(30), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-07"), BigDecimal.valueOf(30), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-06"), BigDecimal.valueOf(30), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-05"), BigDecimal.valueOf(30), "Lønn", false, null)
        )
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, inntekter)

        if (fastsattSykepengegrunnlag !is Vurdering.Avklart) {
            fail { "Expected fastsattSykepengegrunnlag to be Vurdering.Avklart" }
        }

        assertEquals(17, fastsattSykepengegrunnlag.fastsattVerdi)
    }

    @Test
    fun `sammenligningsgrunnlaget er summen av inntekter de siste 12 måneder`() {
        val inntekter = listOf(
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2019-02"), BigDecimal.valueOf(30), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2019-01"), BigDecimal.valueOf(12), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2019-01"), BigDecimal.valueOf(30), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2018-12"), BigDecimal.valueOf(12), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-12"), BigDecimal.valueOf(30), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-11"), BigDecimal.valueOf(25), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-10"), BigDecimal.valueOf(25), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-09"), BigDecimal.valueOf(17), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2018-05"), BigDecimal.valueOf(16), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2018-03"), BigDecimal.valueOf(15), "Lønn", false, null)
        )
        val fastsattSammenligningsgrunnlag = fastsettSammenligningsgrunnlag(inntekter)

        if (fastsattSammenligningsgrunnlag !is Vurdering.Avklart) {
            fail { "Expected fastsattSammenligningsgrunnlag to be Vurdering.Avklart" }
        }

        assertEquals(30+12+30+12+30+25+25+17+16+15, fastsattSammenligningsgrunnlag.fastsattVerdi)
    }

    @Test
    fun `sykepengegrunnlag baseres på arbeidsgiverperioden dersom mindre enn 25% avvik fra sammenlikningsgrunnlaget`() {
        val førsteSykdomsdag = LocalDate.parse("2019-03-01")
        val inntekter = listOf(
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2019-02"), BigDecimal.valueOf(30), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2019-01"), BigDecimal.valueOf(30), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-12"), BigDecimal.valueOf(30), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-11"), BigDecimal.valueOf(29), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-10"), BigDecimal.valueOf(29), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-09"), BigDecimal.valueOf(29), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2018-08"), BigDecimal.valueOf(29), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2018-07"), BigDecimal.valueOf(29), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2018-06"), BigDecimal.valueOf(29), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2018-05"), BigDecimal.valueOf(29), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2017-04"), BigDecimal.valueOf(29), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2017-03"), BigDecimal.valueOf(29), "Lønn", false, null)
        )

        val beregningsgrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, inntekter)
        val sammenligningsgrunnlag = fastsettSammenligningsgrunnlag(inntekter)

        if (beregningsgrunnlag !is Vurdering.Avklart || sammenligningsgrunnlag !is Vurdering.Avklart) {
            fail { "Expected beregningsgrunnlag and sammenligningsgrunnlag to be Vurdering.Avklart" }
        }

        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetNårTrygdenYterSykepenger(sammenligningsgrunnlag, beregningsgrunnlag)

        if (fastsattSykepengegrunnlag !is Vurdering.Avklart) {
            fail { "Expected fastsattSykepengegrunnlag to be Vurdering.Avklart" }
        }

        assertEquals(30 * 12, fastsattSykepengegrunnlag.fastsattVerdi)
    }

    @Test
    fun `sykepengegrunnlag er uavklart dersom mer enn 25% avvik fra sammenlikningsgrunnlaget`() {
        val førsteSykdomsdag = LocalDate.parse("2019-03-01")
        val inntekter = listOf(
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2019-02"), BigDecimal.valueOf(30), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2019-01"), BigDecimal.valueOf(30), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-12"), BigDecimal.valueOf(30), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-11"), BigDecimal.valueOf(20), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-10"), BigDecimal.valueOf(20), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-09"), BigDecimal.valueOf(20), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2018-08"), BigDecimal.valueOf(20), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2018-07"), BigDecimal.valueOf(20), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2018-06"), BigDecimal.valueOf(20), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2018-05"), BigDecimal.valueOf(20), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2017-04"), BigDecimal.valueOf(20), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer, "Organisasjon"), YearMonth.parse("2017-03"), BigDecimal.valueOf(20), "Lønn", false, null)
        )

        val beregningsgrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, inntekter)
        val sammenligningsgrunnlag = fastsettSammenligningsgrunnlag(inntekter)

        if (beregningsgrunnlag !is Vurdering.Avklart || sammenligningsgrunnlag !is Vurdering.Avklart) {
            fail { "Expected beregningsgrunnlag and sammenligningsgrunnlag to be Vurdering.Avklart" }
        }

        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetNårTrygdenYterSykepenger(sammenligningsgrunnlag, beregningsgrunnlag)

        if (fastsattSykepengegrunnlag !is Vurdering.Uavklart) {
            fail { "Expected fastsattSykepengegrunnlag to be Vurdering.Uavklart" }
        }

        assertEquals(Vurdering.Uavklart.Årsak.KREVER_SKJØNNSMESSIG_VURDERING, fastsattSykepengegrunnlag.årsak)
    }


    @Test
    fun `uavklart sykepengegrunnlag når det er færre enn tre inntekter i beregningsperioden`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val inntekter = listOf(
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-12"), BigDecimal.valueOf(1), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-11"), BigDecimal.valueOf(21), "Lønn", false, null)
        )
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, inntekter)

        if (fastsattSykepengegrunnlag !is Vurdering.Uavklart) {
            fail { "Expected fastsattSykepengegrunnlag to be Vurdering.Uavklart" }
        }

        assertEquals("Kan ikke avklare sykepengegrunnlaget fordi vi forventer inntekter fra tre måneder før", fastsattSykepengegrunnlag.begrunnelse)
    }

    @Test
    fun `uavklart sykepengegrunnlag når det ikke er noen inntekter i beregningsperioden`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val inntekter = emptyList<Inntekt>()
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, inntekter)

        if (fastsattSykepengegrunnlag !is Vurdering.Uavklart) {
            fail { "Expected fastsattSykepengegrunnlag to be Vurdering.Uavklart" }
        }

        assertEquals("Kan ikke avklare sykepengegrunnlaget fordi det ikke er inntekter i beregningsperioden", fastsattSykepengegrunnlag.begrunnelse)
    }

    @Test
    fun `uavklart sykepengegrunnlag når det er flere enn tre inntekter i beregningsperioden`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val inntekter = listOf(
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-12"), BigDecimal.valueOf(1), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-12"), BigDecimal.valueOf(10), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-11"), BigDecimal.valueOf(21), "Lønn", false, null),
                Inntekt(Inntektsarbeidsgiver(orgnummer, "Organisasjon"), YearMonth.parse("2018-11"), BigDecimal.valueOf(31), "Lønn", false, null)
        )
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, inntekter)

        if (fastsattSykepengegrunnlag !is Vurdering.Uavklart) {
            fail { "Expected fastsattSykepengegrunnlag to be Vurdering.Uavklart" }
        }

        assertEquals("Kan ikke avklare sykepengegrunnlaget fordi vi forventer inntekter fra tre måneder før", fastsattSykepengegrunnlag.begrunnelse)
    }

}
