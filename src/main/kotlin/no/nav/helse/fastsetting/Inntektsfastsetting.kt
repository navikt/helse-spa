package no.nav.helse.fastsetting

import no.nav.helse.domain.Arbeidsgiver
import no.nav.helse.oppslag.Inntekt
import no.nav.helse.sykepenger.beregning.longValueExact
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

fun LocalDate.yearMonth() = YearMonth.of(year, month.value)

fun fastsettingAvSykepengegrunnlaget(førsteSykdomsdag: LocalDate, arbeidsgiver: Arbeidsgiver, beregningsgrunnlag: List<Inntekt>, sammenligningsgrunnlag: List<Inntekt>): Vurdering<*, *> {
    val sykepengegrunnlagIArbeidsgiverperioden = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag,
            arbeidsgiver, beregningsgrunnlag)

    if (sykepengegrunnlagIArbeidsgiverperioden is Vurdering.Uavklart) {
        return sykepengegrunnlagIArbeidsgiverperioden
    }

    val fastsattSammenligningsgrunnlag = fastsettSammenligningsgrunnlag(førsteSykdomsdag, sammenligningsgrunnlag)

    if (fastsattSammenligningsgrunnlag is Vurdering.Uavklart) {
        return fastsattSammenligningsgrunnlag
    }

    val sykepengegrunnlagNårTrygdenYter = fastsettingAvSykepengegrunnlagetNårTrygdenYterSykepenger(fastsattSammenligningsgrunnlag as Vurdering.Avklart, sykepengegrunnlagIArbeidsgiverperioden as Vurdering.Avklart)

    if (sykepengegrunnlagNårTrygdenYter is Vurdering.Uavklart) {
        return sykepengegrunnlagNårTrygdenYter
    }

    return Vurdering.Avklart(Sykepengegrunnlag(sykepengegrunnlagNårTrygdenYter as Vurdering.Avklart, sykepengegrunnlagIArbeidsgiverperioden), "", fastsattSammenligningsgrunnlag.grunnlag , "SPA")
}

const val paragraf_8_28_tredje_ledd_bokstav_a = "§ 8-28 tredje ledd bokstav a) – De tre siste kalendermånedene før arbeidstakeren ble arbeidsufør"
const val paragraf_8_28_andre_ledd = "§ 8-28 andre ledd"
// https://lovdata.no/lov/1997-02-28-19/§8-28
fun fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag: LocalDate, arbeidsgiver: Arbeidsgiver, inntekter: List<Inntekt>): Vurdering<Long, Beregningsperiode> {
    val enMånedFør = førsteSykdomsdag.minusMonths(1)
            .yearMonth()
    val treMånederFør = førsteSykdomsdag.minusMonths(3)
            .yearMonth()

    val beregningsperiode = inntekter.filter { inntekt ->
        inntekt.opptjeningsperiode.fom in treMånederFør.atDay(1)..enMånedFør.atEndOfMonth()
                && inntekt.opptjeningsperiode.tom in treMånederFør.atDay(1)..enMånedFør.atEndOfMonth()
    }.let {
        Beregningsperiode(it, paragraf_8_28_tredje_ledd_bokstav_a + "(${førsteSykdomsdag}) legges til grunn.")
    }

    // TODO: sjekke om listen inneholder mer enn tre elementer? (hva om det er rapportert inn to inntekter for en måned?)

    return if (beregningsperiode.inntekter.isEmpty()) {
        Vurdering.Uavklart(Vurdering.Uavklart.Årsak.MANGELFULL_DATAGRUNNLAG, "Kan ikke avklare sykepengegrunnlaget fordi det ikke er inntekter i beregningsperioden", beregningsperiode)
    } else if (beregningsperiode.inntekter.any { it.arbeidsgiver.orgnr != arbeidsgiver.orgnummer }) {
        Vurdering.Uavklart(Vurdering.Uavklart.Årsak.KREVER_SKJØNNSMESSIG_VURDERING, "Kan ikke avklare sykepengegrunnlaget fordi det andre inntekter i arbeidsgiverperioden enn i fra aktuell arbeidsgiver", beregningsperiode)
    } else if (beregningsperiode.inntekter.size > 3) {
        Vurdering.Uavklart(Vurdering.Uavklart.Årsak.DÅRLIG_DATAGRUNNLAG, "Kan ikke avklare sykepengegrunnlaget fordi det er ${beregningsperiode.inntekter.size} inntekter i beregningsperioden, vi forventer tre eller færre.", beregningsperiode)
    } else {
        // § 8-28 andre ledd
        val aktuellMånedsinntekt = beregningsperiode.inntekter.sumBy { periode ->
            periode.beløp.toInt()
        } / beregningsperiode.inntekter.size

        Vurdering.Avklart(aktuellMånedsinntekt.toLong(), paragraf_8_28_andre_ledd, beregningsperiode, "SPA")
    }
}

fun fastsettSammenligningsgrunnlag(førsteSykdomsdag: LocalDate, sammenligningsgrunnlag: List<Inntekt>) : Vurdering<Long, Beregningsperiode> {
    val enMånedFør = førsteSykdomsdag.minusMonths(1)
            .yearMonth()
    val tolvMånederFør = førsteSykdomsdag.minusMonths(12)
            .yearMonth()

    val beregningsperiode = sammenligningsgrunnlag.filter { inntekt ->
        inntekt.opptjeningsperiode.fom in tolvMånederFør.atDay(1)..enMånedFør.atEndOfMonth()
                && inntekt.opptjeningsperiode.tom in tolvMånederFør.atDay(1)..enMånedFør.atEndOfMonth()
    }.let {
        Beregningsperiode(it, "§ 8-30 andre ledd – rapportert inntekt (se § 8-29) til a-ordningen etter reglene i a-opplysningsloven de siste tolv kalendermånedene før arbeidsuførheten inntraff (${førsteSykdomsdag}) legges til grunn.")
    }

    return Vurdering.Avklart(beregningsperiode.inntekter
            .map {
                it.beløp
            }.reduce(BigDecimal::add).longValueExact(RoundingMode.HALF_UP), "§ 8-30 andre ledd", beregningsperiode, "SPA")
}

val paragraf_8_30_første_ledd = "§ 8-30 første ledd"
// § 8-30 første ledd
fun fastsettingAvSykepengegrunnlagetNårTrygdenYterSykepenger(sammenligningsgrunnlag: Vurdering.Avklart<Long, Beregningsperiode>,
                                                             beregnetAktuellMånedsinntekt: Vurdering.Avklart<Long, Beregningsperiode>): Vurdering<Long, Beregningsperiode> {
    val omregnetÅrsinntekt = beregnetAktuellMånedsinntekt.fastsattVerdi * 12

    val rapportertInntekt = sammenligningsgrunnlag.fastsattVerdi
    val avvik = Math.abs(omregnetÅrsinntekt - rapportertInntekt) / rapportertInntekt.toDouble()

    if (avvik > 0.25) {
        return Vurdering.Uavklart(Vurdering.Uavklart.Årsak.KREVER_SKJØNNSMESSIG_VURDERING,
                "§ 8-30 andre ledd - Sykepengegrunnlaget skal fastsettes ved skjønn fordi omregner årsinntekt ($omregnetÅrsinntekt) avviker mer enn 25% (${avvik * 100}%) fra rapportert inntekt ($rapportertInntekt)",
                sammenligningsgrunnlag.grunnlag) // FIXME: hva ønsker vi egentlig som referert grunnlag her ?
    }

    return Vurdering.Avklart(omregnetÅrsinntekt, paragraf_8_30_første_ledd, beregnetAktuellMånedsinntekt.grunnlag, "SPA")
}

data class Beregningsperiode(val inntekter: List<Inntekt>, val begrunnelse: String)

data class Sykepengegrunnlag(val sykepengegrunnlagNårTrygdenYter: Vurdering.Avklart<Long, Beregningsperiode>, val sykepengegrunnlagIArbeidsgiverperioden: Vurdering.Avklart<Long, Beregningsperiode>)

