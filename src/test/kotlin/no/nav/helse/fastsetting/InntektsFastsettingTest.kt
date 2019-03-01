package no.nav.helse.fastsetting

import no.nav.helse.domain.*
import no.nav.helse.oppslag.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.math.*
import java.time.*

class InntektsFastsettingTest {

    val orgnummer = "974652269"
    val annetOrgnummer = "985538859"

    @Test
    fun `gjennomsnittet av de tre siste kalendermånedene før arbeidsuførhet skal legges til grunn`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val inntekter = listOf(
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-12-01"), LocalDate.parse("2018-12-31")), BigDecimal.valueOf(1)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-11-01"), LocalDate.parse("2018-11-30")), BigDecimal.valueOf(21)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-10-01"), LocalDate.parse("2018-10-31")), BigDecimal.valueOf(29)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-09-01"), LocalDate.parse("2018-09-30")), BigDecimal.valueOf(30)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-08-01"), LocalDate.parse("2018-08-31")), BigDecimal.valueOf(30)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-07-01"), LocalDate.parse("2018-07-31")), BigDecimal.valueOf(30)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-06-01"), LocalDate.parse("2018-06-30")), BigDecimal.valueOf(30)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-05-01"), LocalDate.parse("2018-05-31")), BigDecimal.valueOf(30))
        )
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, Arbeidsgiver("NAV OSLO", orgnummer), inntekter)

        if (fastsattSykepengegrunnlag !is Vurdering.Avklart) {
            fail { "Expected fastsattSykepengegrunnlag to be Vurdering.Avklart" }
        }

        assertEquals(17, fastsattSykepengegrunnlag.fastsattVerdi)
    }

    @Test
    fun `uavklart ved inntekt fra andre arbeidsgivere i arbeidsgiverperioden`() {
        val førsteSykdomsdag = LocalDate.parse("2019-03-01")
        val inntekter = listOf(
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2019-02-01"), LocalDate.parse("2019-02-28")), BigDecimal.valueOf(30)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-01-31")), BigDecimal.valueOf(12))
        )
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, Arbeidsgiver("NAV OSLO", orgnummer), inntekter)

        if (fastsattSykepengegrunnlag !is Vurdering.Uavklart) {
            fail { "Expected fastsattSykepengegrunnlag to be Vurdering.Uavklart" }
        }

        assertEquals("Kan ikke avklare sykepengegrunnlaget fordi det andre inntekter i arbeidsgiverperioden enn i fra aktuell arbeidsgiver", fastsattSykepengegrunnlag.begrunnelse)
    }

    @Test
    fun `sammenligningsgrunnlaget er summen av inntekter de siste 12 måneder`() {
        val førsteSykdomsdag = LocalDate.parse("2019-03-01")
        val inntekter = listOf(
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2019-02-01"), LocalDate.parse("2019-02-28")), BigDecimal.valueOf(30)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-01-31")), BigDecimal.valueOf(12)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-01-31")), BigDecimal.valueOf(30)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2018-12-01"), LocalDate.parse("2018-12-31")), BigDecimal.valueOf(12)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-12-01"), LocalDate.parse("2018-12-31")), BigDecimal.valueOf(30)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-11-01"), LocalDate.parse("2018-11-30")), BigDecimal.valueOf(25)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-10-01"), LocalDate.parse("2018-10-31")), BigDecimal.valueOf(25)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-09-01"), LocalDate.parse("2018-09-30")), BigDecimal.valueOf(17)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2018-05-01"), LocalDate.parse("2018-05-31")), BigDecimal.valueOf(16)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2018-03-01"), LocalDate.parse("2018-03-31")), BigDecimal.valueOf(15)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2018-02-01"), LocalDate.parse("2018-02-28")), BigDecimal.valueOf(14)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2018-01-01"), LocalDate.parse("2018-01-31")), BigDecimal.valueOf(13)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2017-12-01"), LocalDate.parse("2017-12-31")), BigDecimal.valueOf(12))
        )
        val fastsattSammenligningsgrunnlag = fastsettSammenligningsgrunnlag(førsteSykdomsdag, inntekter)

        if (fastsattSammenligningsgrunnlag !is Vurdering.Avklart) {
            fail { "Expected fastsattSammenligningsgrunnlag to be Vurdering.Avklart" }
        }

        assertEquals(30+12+30+12+30+25+25+17+16+15, fastsattSammenligningsgrunnlag.fastsattVerdi)
    }

    @Test
    fun `sykepengegrunnlag baseres på arbeidsgiverperioden dersom mindre enn 25% avvik fra sammenlikningsgrunnlaget`() {
        val førsteSykdomsdag = LocalDate.parse("2019-03-01")
        val inntekter = listOf(
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2019-02-01"), LocalDate.parse("2019-02-28")), BigDecimal.valueOf(30)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-01-31")), BigDecimal.valueOf(30)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-12-01"), LocalDate.parse("2018-12-31")), BigDecimal.valueOf(30)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-11-01"), LocalDate.parse("2018-11-30")), BigDecimal.valueOf(29)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-10-01"), LocalDate.parse("2018-10-31")), BigDecimal.valueOf(29)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-09-01"), LocalDate.parse("2018-09-30")), BigDecimal.valueOf(29)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2018-08-01"), LocalDate.parse("2018-08-31")), BigDecimal.valueOf(29)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2018-07-01"), LocalDate.parse("2018-07-31")), BigDecimal.valueOf(29)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2018-06-01"), LocalDate.parse("2018-06-30")), BigDecimal.valueOf(29)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2018-05-01"), LocalDate.parse("2018-05-31")), BigDecimal.valueOf(29)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2017-04-01"), LocalDate.parse("2017-04-30")), BigDecimal.valueOf(29)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2017-03-01"), LocalDate.parse("2017-03-31")), BigDecimal.valueOf(29))
        )

        val beregningsgrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, Arbeidsgiver("NAV OSLO", orgnummer), inntekter)
        val sammenligningsgrunnlag = fastsettSammenligningsgrunnlag(førsteSykdomsdag, inntekter)

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
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2019-02-01"), LocalDate.parse("2019-02-28")), BigDecimal.valueOf(30)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-01-31")), BigDecimal.valueOf(30)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-12-01"), LocalDate.parse("2018-12-31")), BigDecimal.valueOf(30)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-11-01"), LocalDate.parse("2018-11-30")), BigDecimal.valueOf(20)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-10-01"), LocalDate.parse("2018-10-31")), BigDecimal.valueOf(20)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-09-01"), LocalDate.parse("2018-09-30")), BigDecimal.valueOf(20)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2018-08-01"), LocalDate.parse("2018-08-31")), BigDecimal.valueOf(20)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2018-07-01"), LocalDate.parse("2018-07-31")), BigDecimal.valueOf(20)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2018-06-01"), LocalDate.parse("2018-06-30")), BigDecimal.valueOf(20)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2018-05-01"), LocalDate.parse("2018-05-31")), BigDecimal.valueOf(20)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2017-04-01"), LocalDate.parse("2017-04-30")), BigDecimal.valueOf(20)),
                Inntekt(Inntektsarbeidsgiver(annetOrgnummer), Opptjeningsperiode(LocalDate.parse("2017-03-01"), LocalDate.parse("2017-03-31")), BigDecimal.valueOf(20))
        )

        val beregningsgrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, Arbeidsgiver("NAV OSLO", orgnummer), inntekter)
        val sammenligningsgrunnlag = fastsettSammenligningsgrunnlag(førsteSykdomsdag, inntekter)

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
    fun `gjennomsnittet av kortere periode skal legges til grunn`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val inntekter = listOf(
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-12-01"), LocalDate.parse("2018-12-31")), BigDecimal.valueOf(1)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-11-01"), LocalDate.parse("2018-11-30")), BigDecimal.valueOf(21))
        )
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, Arbeidsgiver("NAV OSLO", orgnummer), inntekter)

        if (fastsattSykepengegrunnlag !is Vurdering.Avklart) {
            fail { "Expected fastsattSykepengegrunnlag to be Vurdering.Avklart" }
        }

        assertEquals(11, fastsattSykepengegrunnlag.fastsattVerdi)
    }

    @Test
    fun `uavklart sykepengegrunnlag når det ikke er noen inntekter i beregningsperioden`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val inntekter = emptyList<Inntekt>()
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, Arbeidsgiver("NAV OSLO", orgnummer), inntekter)

        if (fastsattSykepengegrunnlag !is Vurdering.Uavklart) {
            fail { "Expected fastsattSykepengegrunnlag to be Vurdering.Uavklart" }
        }

        Assertions.assertEquals("Kan ikke avklare sykepengegrunnlaget fordi det ikke er inntekter i beregningsperioden", fastsattSykepengegrunnlag.begrunnelse)
    }

    @Test
    fun `uavklart sykepengegrunnlag når det er flere enn tre inntekter i beregningsperioden`() {
        val førsteSykdomsdag = LocalDate.parse("2019-01-01")
        val inntekter = listOf(
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-12-01"), LocalDate.parse("2018-12-31")), BigDecimal.valueOf(1)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-12-01"), LocalDate.parse("2018-12-31")), BigDecimal.valueOf(10)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-11-01"), LocalDate.parse("2018-11-30")), BigDecimal.valueOf(21)),
                Inntekt(Inntektsarbeidsgiver(orgnummer), Opptjeningsperiode(LocalDate.parse("2018-11-01"), LocalDate.parse("2018-11-30")), BigDecimal.valueOf(31))
        )
        val fastsattSykepengegrunnlag = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, Arbeidsgiver("NAV OSLO", orgnummer), inntekter)

        if (fastsattSykepengegrunnlag !is Vurdering.Uavklart) {
            fail { "Expected fastsattSykepengegrunnlag to be Vurdering.Uavklart" }
        }

        Assertions.assertEquals("Kan ikke avklare sykepengegrunnlaget fordi det er 4 inntekter i beregningsperioden, vi forventer tre eller færre.", fastsattSykepengegrunnlag.begrunnelse)
    }

}
