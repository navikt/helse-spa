package no.nav.helse

import arrow.core.Either
import no.nav.helse.behandling.AvklarteVerdier
import no.nav.helse.behandling.Behandlingsgrunnlag
import no.nav.helse.behandling.Tpsfakta
import no.nav.helse.behandling.sykepengeBeregning
import no.nav.helse.behandling.søknad.Sykepengesøknad
import no.nav.helse.behandling.søknad.tilSakskompleks
import no.nav.helse.dto.*
import no.nav.helse.fastsetting.Aldersgrunnlag
import no.nav.helse.fastsetting.Opptjeningsgrunnlag
import no.nav.helse.fastsetting.Sykepengegrunnlag
import no.nav.helse.fastsetting.Vurdering.Avklart
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsforholdDTO
import no.nav.helse.oppslag.getGrunnbeløpForDato
import no.nav.nare.core.evaluations.Evaluering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDate.parse
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class BeregningTest {

    val fom = parse("2019-01-01")
    val gjeldendeGrunnbeløp = getGrunnbeløpForDato(fom)

    @Test
    fun `skal beregne for 50% grad`() {
        val sakskompleks = vilkårsprøvdSakskompleks(fom, parse("2019-01-02"), 400000, 50)
        val beregningsresultat = (sykepengeBeregning(sakskompleks) as Either.Right).b.beregning

        assertEquals(2, beregningsresultat.dagsatser.size)
        assertEquals((400000 / 260) / 2, beregningsresultat.dagsatser[0].sats)
    }

    @Test
    fun `skal beregne for 100% grad`() {
        val soknad = vilkårsprøvdSakskompleks(parse("2019-01-01"), parse("2019-01-02"), 500000, 100)
        val beregningsresultat = (sykepengeBeregning(soknad) as Either.Right).b.beregning

        assertEquals(2, beregningsresultat.dagsatser.size)
        assertEquals((500000 / 260), beregningsresultat.dagsatser[0].sats)
    }

    @Test
    fun `skal skrelle av ved 6G`() {
        val soknad = vilkårsprøvdSakskompleks(parse("2019-01-01"), parse("2019-01-02"), 10 * gjeldendeGrunnbeløp, 100)
        val beregningsresultat = (sykepengeBeregning(soknad) as Either.Right).b.beregning

        assertEquals(2, beregningsresultat.dagsatser.size)
        assertEquals(BigDecimal.valueOf(6 * gjeldendeGrunnbeløp).divide(BigDecimal(260), 0, RoundingMode.HALF_UP).longValueExact(),
                beregningsresultat.dagsatser[0].sats)
    }

    @Test
    fun `skal ikke utbetale dagsats ved ferie`() {
        val fravær = listOf(FravarDTO(parse("2019-01-04"), parse("2019-01-08"), FravarstypeDTO.FERIE))
        val soknad = vilkårsprøvdSakskompleks(parse("2019-01-01"), parse("2019-01-10"), 500000, 100, fravær)
        val beregningsresultat = (sykepengeBeregning(soknad) as Either.Right).b.beregning

        assertEquals(8, beregningsresultat.dagsatser.size)
        assertEquals(5, beregningsresultat.dagsatser.filter { it.skalUtbetales }.size)
    }

    fun vilkårsprøvdSakskompleks(fom: LocalDate, tom: LocalDate, årslønn: Long, sykmeldingsgrad: Int, fravær: List<FravarDTO> = emptyList()) =
            Behandlingsgrunnlag(
                    Sykepengesøknad(SykepengesøknadV2DTO(
                            id = "1",
                            aktorId = "123123",
                            type = SoknadstypeDTO.ARBEIDSTAKERE,
                            fom = fom,
                            tom = tom,
                            arbeidsgiver = ArbeidsgiverDTO("TheWorkplace", "999888777"),
                            sendtNav = LocalDateTime.ofEpochSecond(parse("2019-01-31").toEpochSecond(LocalTime.NOON, ZoneOffset.UTC), 0, ZoneOffset.UTC),
                            soknadsperioder = listOf(SoknadsperiodeDTO(parse("2019-01-05"), parse("2019-01-31"), sykmeldingsgrad = sykmeldingsgrad)),
                            soktUtenlandsopphold = false,
                            startSyketilfelle = parse("2018-12-01"),
                            status = SoknadsstatusDTO.SENDT,
                            andreInntektskilder = emptyList(),
                            fravar = fravær).asJsonNode()).tilSakskompleks(),
                    faktagrunnlag = faktagrunnlagUtenVerdi,
                    avklarteVerdier = AvklarteVerdier(
                            medlemsskap = Avklart(fastsattVerdi = true, begrunnelse = "derfor", fastsattAv = "test", grunnlag = Tpsfakta(LocalDate.parse("1980-01-01"), "NOR", "NOR", "BOSA", null)),
                            sykepengegrunnlag = Avklart(fastsattVerdi =
                            Sykepengegrunnlag(
                                    sykepengegrunnlagNårTrygdenYter = Avklart(fastsattVerdi = årslønn, grunnlag = emptyList(), begrunnelse = "derfor", fastsattAv = "test"),
                                    sykepengegrunnlagIArbeidsgiverperioden = Avklart(fastsattVerdi = årslønn, grunnlag = emptyList(), begrunnelse = "derfor", fastsattAv = "test")),
                                    fastsattAv = "test",
                                    begrunnelse = "derfor",
                                    grunnlag = emptyList()),
                            alder = Avklart(fastsattVerdi = 40, grunnlag = Aldersgrunnlag(parse("1979-01-01")), begrunnelse = "derfor", fastsattAv = "test"),
                            arbeidsforhold = Avklart(fastsattVerdi = ArbeidsforholdDTO("", no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsgiverDTO("", ""), now(), null), grunnlag = arbeidsforholdUtenVerdi, begrunnelse = "derfor", fastsattAv = "test"),
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
                            opptjeningstid = Avklart(fastsattVerdi = 20, grunnlag = Opptjeningsgrunnlag(førsteSykdomsdag = parse("2018-12-01"), arbeidsforhold = Avklart(fastsattVerdi = ArbeidsforholdDTO("", no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsgiverDTO("", ""), now(), null), grunnlag = arbeidsforholdUtenVerdi, begrunnelse = "derfor", fastsattAv = "test")), begrunnelse = "defor", fastsattAv = "test")),
                    vilkårsprøving = Evaluering.ja("claro"))

}
