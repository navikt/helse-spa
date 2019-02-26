package no.nav.helse.fastsetting

import no.nav.helse.Arbeidsgiver
import no.nav.helse.Inntekt
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InntektsFastsettingTest {

    val orgnummer = "974652269"
    val annetOrgnummer = "985538859"

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
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, Arbeidsgiver("NAV OSLO", orgnummer), inntekter)

        Assertions.assertEquals(17, (fastsattSykepengegrunnlag as Vurdering.Avklart).fastsattVerdi.aktuellMånedsinntekt)
    }

    @Test
    fun `uavklart ved inntekt fra andre arbeidsgivere i arbeidsgiverperioden`() {
        val førsteSykdomsdag = LocalDate.parse("2019-03-01")
        val inntekter = listOf(
                Inntekt(LocalDate.parse("2019-02-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2019-01-01").yearMonth(), 12, annetOrgnummer),
                Inntekt(LocalDate.parse("2019-01-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2018-12-01").yearMonth(), 12, annetOrgnummer),
                Inntekt(LocalDate.parse("2018-12-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2018-11-01").yearMonth(), 25, orgnummer),
                Inntekt(LocalDate.parse("2018-10-01").yearMonth(), 25, orgnummer),
                Inntekt(LocalDate.parse("2018-09-01").yearMonth(), 20, orgnummer)
        )
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, Arbeidsgiver("NAV OSLO", orgnummer), inntekter)

        Assertions.assertEquals(Vurdering.Uavklart::class, fastsattSykepengegrunnlag::class)
        Assertions.assertEquals("Kan ikke avklare sykepengegrunnlaget fordi det andre inntekter i arbeidsgiverperioden enn i fra aktuell arbeidsgiver", (fastsattSykepengegrunnlag as Vurdering.Uavklart).begrunnelse)
    }

    @Test
    fun `sammenligningsgrunnlaget er summen av inntekter de siste 12 måneder`() {
        val førsteSykdomsdag = LocalDate.parse("2019-03-01")
        val inntekter = listOf(
                Inntekt(LocalDate.parse("2019-02-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2019-01-01").yearMonth(), 12, annetOrgnummer),
                Inntekt(LocalDate.parse("2019-01-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2018-12-01").yearMonth(), 12, annetOrgnummer),
                Inntekt(LocalDate.parse("2018-12-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2018-11-01").yearMonth(), 25, orgnummer),
                Inntekt(LocalDate.parse("2018-10-01").yearMonth(), 25, orgnummer),
                Inntekt(LocalDate.parse("2018-09-01").yearMonth(), 17, orgnummer),
                Inntekt(LocalDate.parse("2018-05-01").yearMonth(), 16, annetOrgnummer),
                Inntekt(LocalDate.parse("2018-03-01").yearMonth(), 15, annetOrgnummer),
                Inntekt(LocalDate.parse("2018-02-01").yearMonth(), 14, annetOrgnummer),
                Inntekt(LocalDate.parse("2018-01-01").yearMonth(), 13, annetOrgnummer),
                Inntekt(LocalDate.parse("2017-12-01").yearMonth(), 12, annetOrgnummer)
        )
        val fastsattSammenligningsgrunnlag = fastsettSammenligningsgrunnlag(førsteSykdomsdag, inntekter)

        Assertions.assertEquals(30+12+30+12+30+25+25+17+16+15,
                (fastsattSammenligningsgrunnlag as Vurdering.Avklart).fastsattVerdi.rapportertInntektSiste12Måneder)
    }

    @Test
    fun `sykepengegrunnlag baseres på arbeidsgiverperioden dersom mindre enn 25% avvik fra sammenlikningsgrunnlaget`() {
        val førsteSykdomsdag = LocalDate.parse("2019-03-01")
        val inntekter = listOf(
                Inntekt(LocalDate.parse("2019-02-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2019-01-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2018-12-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2018-11-01").yearMonth(), 29, orgnummer),
                Inntekt(LocalDate.parse("2018-10-01").yearMonth(), 29, orgnummer),
                Inntekt(LocalDate.parse("2018-09-01").yearMonth(), 29, orgnummer),
                Inntekt(LocalDate.parse("2018-08-01").yearMonth(), 29, annetOrgnummer),
                Inntekt(LocalDate.parse("2018-07-01").yearMonth(), 29, annetOrgnummer),
                Inntekt(LocalDate.parse("2018-06-01").yearMonth(), 29, annetOrgnummer),
                Inntekt(LocalDate.parse("2018-05-01").yearMonth(), 29, annetOrgnummer),
                Inntekt(LocalDate.parse("2017-04-01").yearMonth(), 29, annetOrgnummer),
                Inntekt(LocalDate.parse("2017-03-01").yearMonth(), 29, annetOrgnummer)
        )

        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetNårTrygdenYterSykepenger(førsteSykdomsdag, Arbeidsgiver("NAV OSLO", orgnummer), inntekter)

        println(fastsattSykepengegrunnlag)

        Assertions.assertEquals(Vurdering.Avklart::class, fastsattSykepengegrunnlag::class)
        Assertions.assertEquals(30 * 12, (fastsattSykepengegrunnlag as Vurdering.Avklart).fastsattVerdi.fastsattÅrsinntektSomSykepengegrunnlag)
    }

    @Test
    fun `sykepengegrunnlag er uavklart dersom mer enn 25% avvik fra sammenlikningsgrunnlaget`() {
        val førsteSykdomsdag = LocalDate.parse("2019-03-01")
        val inntekter = listOf(
                Inntekt(LocalDate.parse("2019-02-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2019-01-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2018-12-01").yearMonth(), 30, orgnummer),
                Inntekt(LocalDate.parse("2018-11-01").yearMonth(), 20, orgnummer),
                Inntekt(LocalDate.parse("2018-10-01").yearMonth(), 20, orgnummer),
                Inntekt(LocalDate.parse("2018-09-01").yearMonth(), 20, orgnummer),
                Inntekt(LocalDate.parse("2018-08-01").yearMonth(), 20, annetOrgnummer),
                Inntekt(LocalDate.parse("2018-07-01").yearMonth(), 20, annetOrgnummer),
                Inntekt(LocalDate.parse("2018-06-01").yearMonth(), 20, annetOrgnummer),
                Inntekt(LocalDate.parse("2018-05-01").yearMonth(), 20, annetOrgnummer),
                Inntekt(LocalDate.parse("2017-04-01").yearMonth(), 20, annetOrgnummer),
                Inntekt(LocalDate.parse("2017-03-01").yearMonth(), 20, annetOrgnummer)
        )

        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetNårTrygdenYterSykepenger(førsteSykdomsdag, Arbeidsgiver("NAV OSLO", orgnummer), inntekter)
        println(fastsattSykepengegrunnlag)

        Assertions.assertEquals(Vurdering.Uavklart::class, fastsattSykepengegrunnlag::class)
        Assertions.assertEquals(Vurdering.Uavklart.Årsak.KREVER_SKJØNNSMESSIG_VURDERING, (fastsattSykepengegrunnlag as Vurdering.Uavklart).årsak)
    }


    @Test
    fun `gjennomsnittet av kortere periode skal legges til grunn`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val inntekter = listOf(
                Inntekt(LocalDate.parse("2018-12-01").yearMonth(), 1, orgnummer),
                Inntekt(LocalDate.parse("2018-11-01").yearMonth(), 21, orgnummer)
        )
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, Arbeidsgiver("NAV OSLO", orgnummer), inntekter)

        Assertions.assertEquals(11, (fastsattSykepengegrunnlag as Vurdering.Avklart).fastsattVerdi.aktuellMånedsinntekt)
    }

    @Test
    fun `uavklart sykepengegrunnlag når det ikke er noen inntekter i beregningsperioden`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val inntekter = emptyList<Inntekt>()
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, Arbeidsgiver("NAV OSLO", orgnummer), inntekter)

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
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, Arbeidsgiver("NAV OSLO", orgnummer), inntekter)

        Assertions.assertEquals(Vurdering.Uavklart::class, fastsattSykepengegrunnlag::class)
        Assertions.assertEquals("Kan ikke avklare sykepengegrunnlaget fordi det er 4 inntekter i beregningsperioden, vi forventer tre eller færre.", (fastsattSykepengegrunnlag as Vurdering.Uavklart).begrunnelse)
    }

}
