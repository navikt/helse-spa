package no.nav.helse

import no.nav.helse.behandling.*
import no.nav.helse.domain.ArbeidsgiverFraSøknad
import no.nav.helse.fastsetting.Aldersgrunnlag
import no.nav.helse.fastsetting.Beregningsperiode
import no.nav.helse.fastsetting.Opptjeningsgrunnlag
import no.nav.helse.fastsetting.Sykepengegrunnlag
import no.nav.helse.fastsetting.Vurdering.Avklart
import no.nav.helse.oppslag.getGrunnbeløpForDato
import no.nav.nare.core.evaluations.Evaluering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDate.parse
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class BeregningTest {

    val fom = parse("2019-01-01")
    val gjeldendeGrunnbeløp = getGrunnbeløpForDato(fom)

    @Test
    fun `skal beregne for 50% grad`() {
        val soknad = vilkårsprøvdSøknad(fom, parse("2019-01-02"), 400000, 50)
        val beregningsresultat = (sykepengeBeregning(soknad) as Either.Right).right.beregning

        assertEquals(2, beregningsresultat.dagsatser.size)
        assertEquals((400000 / 260) / 2, beregningsresultat.dagsatser[0].sats)
    }

    @Test
    fun `skal beregne for 100% grad`() {
        val soknad = vilkårsprøvdSøknad(parse("2019-01-01"), parse("2019-01-02"), 500000, 100)
        val beregningsresultat = (sykepengeBeregning(soknad) as Either.Right).right.beregning

        assertEquals(2, beregningsresultat.dagsatser.size)
        assertEquals((500000 / 260), beregningsresultat.dagsatser[0].sats)
    }

    @Test
    fun `skal skrelle av ved 6G`() {
        val soknad = vilkårsprøvdSøknad(parse("2019-01-01"), parse("2019-01-02"), 10 * gjeldendeGrunnbeløp, 100)
        val beregningsresultat = (sykepengeBeregning(soknad) as Either.Right).right.beregning

        assertEquals(2, beregningsresultat.dagsatser.size)
        assertEquals(BigDecimal.valueOf(6 * gjeldendeGrunnbeløp).divide(BigDecimal(260), 0, RoundingMode.HALF_UP).longValueExact(),
                beregningsresultat.dagsatser[0].sats)
    }

    fun vilkårsprøvdSøknad(fom: LocalDate, tom: LocalDate, årslønn: Long, sykmeldingsgrad: Int) =
            Behandlingsgrunnlag(
                    originalSøknad = Sykepengesøknad(
                            id = "1",
                            aktorId = "123123",
                            type = "ARBEIDSTAKERE",
                            fom = fom,
                            tom = tom,
                            arbeidsgiver = ArbeidsgiverFraSøknad("TheWorkplace", "999888777"),
                            arbeidsgiverForskutterer = true,
                            sendtNav = LocalDateTime.ofEpochSecond(parse("2019-01-31").toEpochSecond(LocalTime.NOON, ZoneOffset.UTC), 0, ZoneOffset.UTC),
                            soknadsperioder = listOf(Soknadsperiode(parse("2019-01-05"), parse("2019-01-31"), sykmeldingsgrad = sykmeldingsgrad)),
                            soktUtenlandsopphold = false,
                            startSyketilfelle = parse("2018-12-01"),
                            status = "SENDT",
                            andreInntektskilder = emptyList(),
                            fravær = emptyList()),
                    faktagrunnlag = faktagrunnlagUtenVerdi,
                    avklarteVerdier = AvklarteVerdier(
                            medlemsskap = Avklart(fastsattVerdi = true, begrunnelse = "derfor", fastsattAv = "test", grunnlag = Tpsfakta(LocalDate.parse("1980-01-01"), "NOR", "NOR", "BOSA", null)),
                            sykepengegrunnlag = Avklart(fastsattVerdi =
                            Sykepengegrunnlag(
                                    sykepengegrunnlagNårTrygdenYter = Avklart(fastsattVerdi = årslønn, grunnlag = Beregningsperiode(emptyMap(), "derfor"), begrunnelse = "derfor", fastsattAv = "test"),
                                    sykepengegrunnlagIArbeidsgiverperioden = Avklart(fastsattVerdi = årslønn, grunnlag = Beregningsperiode(emptyMap(), "derfor"), begrunnelse = "derfor", fastsattAv = "test")),
                                    fastsattAv = "test",
                                    begrunnelse = "derfor",
                                    grunnlag = Beregningsperiode(emptyMap(), "derfor")),
                            alder = Avklart(fastsattVerdi = 40, grunnlag = Aldersgrunnlag(parse("1979-01-01")), begrunnelse = "derfor", fastsattAv = "test"),
                            arbeidsforhold = Avklart(fastsattVerdi = true, grunnlag = arbeidsforholdUtenVerdi, begrunnelse = "derfor", fastsattAv = "test"),
                            maksdato = Avklart(fastsattVerdi = parse("2019-03-03"),
                                    grunnlag = Grunnlagsdata(
                                            førsteFraværsdag = parse("2020-01-01"),
                                            førsteSykepengedag = parse("2020-01-01"),
                                            yrkesstatus = Yrkesstatus.ARBEIDSTAKER,
                                            personensAlder = 40,
                                            tidligerePerioder = emptyList()
                                    ),
                                    begrunnelse = "derfor",
                                    fastsattAv = "test"),
                            sykepengehistorikk = emptyList(),
                            opptjeningstid = Avklart(fastsattVerdi = 20, grunnlag = Opptjeningsgrunnlag(førsteSykdomsdag = parse("2018-12-01"), arbeidsforhold = emptyList()), begrunnelse = "defor", fastsattAv = "test")),
                    vilkårsprøving = Evaluering.ja("claro"))

}
