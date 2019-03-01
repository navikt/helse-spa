package no.nav.helse

import no.nav.helse.behandling.*
import no.nav.helse.domain.*
import no.nav.helse.fastsetting.*
import no.nav.helse.fastsetting.Sykepengegrunnlag
import no.nav.helse.sykepenger.beregning.*
import no.nav.nare.core.evaluations.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.math.*
import java.time.*

class BeregningTest {

    @Test
    fun `skal beregne for 50% grad`() {
        val soknad = vilkårsprøvdSøknad(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-01-02"), 400000, 50)
        val beregningsresultat = beregn(lagBeregninggrunnlag(soknad))

        assertEquals(2, beregningsresultat.dagsatser.size)
        assertEquals((400000 / 260) / 2, beregningsresultat.dagsatser[0].sats)
    }

    @Test
    fun `skal beregne for 100% grad`() {
        val soknad = vilkårsprøvdSøknad(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-01-02"), 500000, 100)
        val beregningsresultat = beregn(lagBeregninggrunnlag(soknad))

        assertEquals(2, beregningsresultat.dagsatser.size)
        assertEquals((500000 / 260), beregningsresultat.dagsatser[0].sats)
    }

    @Test
    fun `skal skrelle av ved 6G`() {
        val soknad = vilkårsprøvdSøknad(LocalDate.parse("2019-01-01"), LocalDate.parse("2019-01-02"), 10 * grunnbeløp(), 100)
        val beregningsresultat = beregn(lagBeregninggrunnlag(soknad))

        assertEquals(2, beregningsresultat.dagsatser.size)
        assertEquals(BigDecimal.valueOf(6 * grunnbeløp()).divide(BigDecimal(260), 0, RoundingMode.HALF_UP).longValueExact(),
                beregningsresultat.dagsatser[0].sats)
    }


    fun vilkårsprøvdSøknad(fom: LocalDate, tom: LocalDate, årslønn: Long, sykmeldingsgrad: Int) =
            Vilkårsprøving(
                    originalSøknad = Sykepengesøknad(
                            aktorId = "123123",
                            fom = fom,
                            tom = tom,
                            arbeidsgiver = Arbeidsgiver("TheWorkplace", "999888777"),
                            harVurdertInntekt = false,
                            sendtNav = LocalDateTime.ofEpochSecond(LocalDate.parse("2019-01-31").toEpochSecond(LocalTime.NOON, ZoneOffset.UTC), 0, ZoneOffset.UTC),
                            soknadsperioder = listOf(Soknadsperiode(LocalDate.parse("2019-01-05"), LocalDate.parse("2019-01-31"), sykmeldingsgrad = sykmeldingsgrad)),
                            soktUtenlandsopphold = false,
                            startSyketilfelle = LocalDate.parse("2018-12-01"),
                            status = "SENDT"),
                    faktagrunnlag = faktagrunnlagUtenVerdi,
                    avklarteVerdier = AvklarteVerdier(
                            medlemsskap = Vurdering.Avklart(fastsattVerdi = true, begrunnelse = "derfor", fastsattAv = "test", grunnlag = Medlemsskapgrunnlag("NO")),
                            sykepengegrunnlag = Vurdering.Avklart(fastsattVerdi =
                            Sykepengegrunnlag(
                                    sykepengegrunnlagNårTrygdenYter = Vurdering.Avklart(fastsattVerdi = årslønn, grunnlag = Beregningsperiode(emptyList(), "derfor"), begrunnelse = "derfor", fastsattAv = "test"),
                                    sykepengegrunnlagIArbeidsgiverperioden = Vurdering.Avklart(fastsattVerdi = årslønn, grunnlag = Beregningsperiode(emptyList(), "derfor"), begrunnelse = "derfor", fastsattAv = "test")),
                                    fastsattAv = "test",
                                    begrunnelse = "derfor",
                                    grunnlag = Beregningsperiode(emptyList(), "derfor")),
                            alder = Vurdering.Avklart(fastsattVerdi = 40, grunnlag = Aldersgrunnlag(LocalDate.parse("1979-01-01")), begrunnelse = "derfor", fastsattAv = "test"),
                            arbeidsforhold = Vurdering.Avklart(fastsattVerdi = true, grunnlag = arbeidsforholdUtenVerdi, begrunnelse = "derfor", fastsattAv = "test"),
                            maksdato = Vurdering.Avklart(fastsattVerdi = LocalDate.parse("2019-03-03"), grunnlag = emptyList<LocalDate>(), begrunnelse = "derfor", fastsattAv = "test"),
                            sykepengeliste = emptyList(),
                            opptjeningstid = Vurdering.Avklart(fastsattVerdi = 20, grunnlag = Opptjeningsgrunnlag(førsteSykdomsdag = LocalDate.parse("2018-12-01"), arbeidsforhold = emptyList()), begrunnelse = "defor", fastsattAv = "test")),
                    vilkårsprøving = Evaluering.ja("claro"))

}