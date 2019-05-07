package no.nav.helse.fastsetting

import no.nav.helse.fastsetting.Vurdering.Uavklart
import no.nav.helse.fastsetting.Vurdering.Uavklart.Årsak.HAR_IKKE_DATA
import no.nav.helse.fastsetting.Vurdering.Uavklart.Årsak.KREVER_SKJØNNSMESSIG_VURDERING
import no.nav.helse.oppslag.Inntekt
import no.nav.helse.sykepenger.beregning.longValueExact
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

fun LocalDate.yearMonth() = YearMonth.of(year, month.value)

fun fastsettingAvSykepengegrunnlaget(førsteSykdomsdag: LocalDate, beregningsgrunnlag: List<Inntekt>, sammenligningsgrunnlag: List<Inntekt>): Vurdering<*, List<Inntekt>> {
    val sykepengegrunnlagIArbeidsgiverperioden = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, beregningsgrunnlag)

    if (sykepengegrunnlagIArbeidsgiverperioden is Uavklart) {
        return sykepengegrunnlagIArbeidsgiverperioden
    }

    val fastsattSammenligningsgrunnlag = fastsettSammenligningsgrunnlag(sammenligningsgrunnlag)

    if (fastsattSammenligningsgrunnlag is Uavklart) {
        return fastsattSammenligningsgrunnlag
    }

    val sykepengegrunnlagNårTrygdenYter = fastsettingAvSykepengegrunnlagetNårTrygdenYterSykepenger(fastsattSammenligningsgrunnlag as Vurdering.Avklart, sykepengegrunnlagIArbeidsgiverperioden as Vurdering.Avklart)

    if (sykepengegrunnlagNårTrygdenYter is Uavklart) {
        return sykepengegrunnlagNårTrygdenYter
    }

    return Vurdering.Avklart(Sykepengegrunnlag(sykepengegrunnlagNårTrygdenYter as Vurdering.Avklart, sykepengegrunnlagIArbeidsgiverperioden), "", fastsattSammenligningsgrunnlag.grunnlag , "SPA")
}

const val paragraf_8_28_tredje_ledd_bokstav_a = "§ 8-28 tredje ledd bokstav a) - De tre siste kalendermånedene før arbeidstakeren ble arbeidsufør"
const val paragraf_8_28_andre_ledd = "§ 8-28 andre ledd"
// https://lovdata.no/lov/1997-02-28-19/§8-28
fun fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag: LocalDate, beregningsperiode: List<Inntekt>): Vurdering<Long, List<Inntekt>> {
    if (beregningsperiode.isEmpty()) {
        return Uavklart(HAR_IKKE_DATA, "Ingen inntekter i beregningsperiode", "Kan ikke avklare sykepengegrunnlaget fordi det ikke er inntekter i beregningsperioden", beregningsperiode)
    }

    val enMånedFør = førsteSykdomsdag.minusMonths(1)
            .yearMonth()
    val treMånederFør = førsteSykdomsdag.minusMonths(3)
            .yearMonth()

    val gruppertEtterMåned = beregningsperiode.groupBy { it.utbetalingsperiode }.filter { it.key >= treMånederFør && it.key <= enMånedFør }
    if (gruppertEtterMåned.size != 3 || gruppertEtterMåned.any { it.value.isEmpty() }) {
        return Uavklart(HAR_IKKE_DATA, "Mangler inntekter for minst én av tre foregående måneder", "Kan ikke avklare sykepengegrunnlaget fordi vi forventer inntekter fra tre måneder før", beregningsperiode)
    }

    // § 8-28 andre ledd
    val aktuellMånedsinntekt = gruppertEtterMåned.flatMap { it.value }
            .fold(BigDecimal.ZERO) { acc, current ->
                acc.add(current.beløp)
            }
            .divide(BigDecimal.valueOf(3), RoundingMode.HALF_UP)
            .longValueExact(RoundingMode.HALF_UP)

        return Vurdering.Avklart(aktuellMånedsinntekt, paragraf_8_28_andre_ledd, beregningsperiode, "SPA")
}

fun fastsettSammenligningsgrunnlag(sammenligningsgrunnlag: List<Inntekt>) : Vurdering<Long, List<Inntekt>> {
    return Vurdering.Avklart(sammenligningsgrunnlag
            .fold(BigDecimal.ZERO) { acc, current ->
                acc.add(current.beløp)
            }
            .longValueExact(RoundingMode.HALF_UP), "§ 8-30 andre ledd", sammenligningsgrunnlag, "SPA")
}

val paragraf_8_30_første_ledd = "§ 8-30 første ledd"
// § 8-30 første ledd
fun fastsettingAvSykepengegrunnlagetNårTrygdenYterSykepenger(sammenligningsgrunnlag: Vurdering.Avklart<Long, List<Inntekt>>,
                                                             beregnetAktuellMånedsinntekt: Vurdering.Avklart<Long, List<Inntekt>>): Vurdering<Long, List<Inntekt>> {
    val omregnetÅrsinntekt = beregnetAktuellMånedsinntekt.fastsattVerdi * 12

    val rapportertInntekt = sammenligningsgrunnlag.fastsattVerdi
    val avvik = Math.abs(omregnetÅrsinntekt - rapportertInntekt) / rapportertInntekt.toDouble()

    if (avvik > 0.25) {
        return Uavklart(KREVER_SKJØNNSMESSIG_VURDERING,
                "25% avvik",
                "§ 8-30 andre ledd - Sykepengegrunnlaget skal fastsettes ved skjønn fordi omregner årsinntekt ($omregnetÅrsinntekt) avviker mer enn 25% (${avvik * 100}%) fra rapportert inntekt ($rapportertInntekt)",
                sammenligningsgrunnlag.grunnlag) // FIXME: hva ønsker vi egentlig som referert grunnlag her ?
    }

    return Vurdering.Avklart(omregnetÅrsinntekt, paragraf_8_30_første_ledd, beregnetAktuellMånedsinntekt.grunnlag, "SPA")
}

data class Sykepengegrunnlag(val sykepengegrunnlagNårTrygdenYter: Vurdering.Avklart<Long, List<Inntekt>>, val sykepengegrunnlagIArbeidsgiverperioden: Vurdering.Avklart<Long, List<Inntekt>>)

