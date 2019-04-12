package no.nav.helse.fastsetting

import no.nav.helse.domain.ArbeidsgiverFraSøknad
import no.nav.helse.fastsetting.Vurdering.Uavklart
import no.nav.helse.fastsetting.Vurdering.Uavklart.Årsak.*
import no.nav.helse.oppslag.Inntekt
import no.nav.helse.sykepenger.beregning.longValueExact
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

fun LocalDate.yearMonth() = YearMonth.of(year, month.value)

fun fastsettingAvSykepengegrunnlaget(førsteSykdomsdag: LocalDate, arbeidsgiver: ArbeidsgiverFraSøknad, beregningsgrunnlag: List<Inntekt>, sammenligningsgrunnlag: List<Inntekt>): Vurdering<*, *> {
    val sykepengegrunnlagIArbeidsgiverperioden = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag,
            arbeidsgiver, beregningsgrunnlag)

    if (sykepengegrunnlagIArbeidsgiverperioden is Uavklart) {
        return sykepengegrunnlagIArbeidsgiverperioden
    }

    val fastsattSammenligningsgrunnlag = fastsettSammenligningsgrunnlag(førsteSykdomsdag, sammenligningsgrunnlag)

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
fun fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag: LocalDate, arbeidsgiver: ArbeidsgiverFraSøknad, inntekter: List<Inntekt>): Vurdering<Long, Beregningsperiode> {
    val enMånedFør = førsteSykdomsdag.minusMonths(1)
            .yearMonth()
    val treMånederFør = førsteSykdomsdag.minusMonths(3)
            .yearMonth()

    val beregningsperiode = inntekter.filter { inntekt ->
        inntekt.utbetalingsperiode in treMånederFør..enMånedFør
    }.groupBy { inntekt ->
        inntekt.arbeidsgiver.identifikator
    }.mapValues { entry ->
        entry.value.groupBy {  inntekt ->
            inntekt.utbetalingsperiode
        }
    }.let {
        Beregningsperiode(it, paragraf_8_28_tredje_ledd_bokstav_a + "(${førsteSykdomsdag}) legges til grunn.")
    }

    if (beregningsperiode.inntekter.isEmpty()) {
        return Uavklart(HAR_IKKE_DATA, "Kan ikke avklare sykepengegrunnlaget fordi det ikke er inntekter i beregningsperioden", beregningsperiode)
    } else if (beregningsperiode.inntekter.keys.size > 1) {
        return if (beregningsperiode.inntekter.keys.firstOrNull { it == arbeidsgiver.orgnummer } == null) {
            Uavklart(HAR_IKKE_DATA, "Kan ikke avklare sykepengegrunnlaget fordi det finnes ikke inntekter fra aktuell arbeidsgiver", beregningsperiode)
        } else {
            Uavklart(FORSTÅR_IKKE_DATA, "Kan ikke avklare sykepengegrunnlaget fordi det andre inntekter i arbeidsgiverperioden i tillegg til aktuell arbeidsgiver", beregningsperiode)
        }
    } else if (beregningsperiode.inntekter.keys.firstOrNull { it == arbeidsgiver.orgnummer } == null) {
        return Uavklart(HAR_IKKE_DATA, "Kan ikke avklare sykepengegrunnlaget fordi det finnes ikke inntekter fra aktuell arbeidsgiver", beregningsperiode)
    }

    val inntekterForArbeidsgiver = beregningsperiode.inntekter.getValue(arbeidsgiver.orgnummer)

    if (inntekterForArbeidsgiver.size != 3) {
        return Uavklart(FALLER_UTENFOR_MVP, "Kan ikke avklare sykepengegrunnlaget fordi vi forventer inntekter fra tre måneder", beregningsperiode)
    } else {
        val inntekterFraTreMånederFør = inntekterForArbeidsgiver[treMånederFør]
        val inntekterFraToMånederFør = inntekterForArbeidsgiver[treMånederFør.plusMonths(1)]
        val inntekterFraEnMånedFør = inntekterForArbeidsgiver[treMånederFør.plusMonths(2)]

        if (inntekterFraTreMånederFør == null || inntekterFraToMånederFør == null || inntekterFraEnMånedFør == null) {
            return Uavklart(HAR_IKKE_DATA, "Kan ikke avklare sykepengegrunnlaget fordi vi forventer inntekter fra tre måneder før", beregningsperiode)
        }

        if (inntekterFraTreMånederFør.isEmpty() || inntekterFraToMånederFør.isEmpty() || inntekterFraEnMånedFør.isEmpty()) {
            return Uavklart(HAR_IKKE_DATA, "Kan ikke avklare sykepengegrunnlaget fordi vi forventer inntekter fra tre måneder før", beregningsperiode)
        }

        val sumInntekterTreMånederFør = inntekterFraTreMånederFør.fold(BigDecimal.ZERO) { acc, current ->
            acc.add(current.beløp)
        }
        val sumInntekterToMånederFør = inntekterFraToMånederFør.fold(BigDecimal.ZERO) { acc, current ->
            acc.add(current.beløp)
        }
        val sumInntekterEnMånedFør = inntekterFraEnMånedFør.fold(BigDecimal.ZERO) { acc, current ->
            acc.add(current.beløp)
        }

        // § 8-28 andre ledd
        val aktuellMånedsinntekt = sumInntekterTreMånederFør
                .add(sumInntekterToMånederFør)
                .add(sumInntekterEnMånedFør)
                .divide(BigDecimal.valueOf(3), RoundingMode.HALF_UP)
                .longValueExact(RoundingMode.HALF_UP)

        return Vurdering.Avklart(aktuellMånedsinntekt, paragraf_8_28_andre_ledd, beregningsperiode, "SPA")
    }
}

fun fastsettSammenligningsgrunnlag(førsteSykdomsdag: LocalDate, sammenligningsgrunnlag: List<Inntekt>) : Vurdering<Long, Beregningsperiode> {
    val enMånedFør = førsteSykdomsdag.minusMonths(1)
            .yearMonth()
    val tolvMånederFør = førsteSykdomsdag.minusMonths(12)
            .yearMonth()

    val beregningsperiode = sammenligningsgrunnlag.filter { inntekt ->
        inntekt.utbetalingsperiode in tolvMånederFør..enMånedFør
    }.groupBy { inntekt ->
        inntekt.arbeidsgiver.identifikator
    }.mapValues { entry ->
        entry.value.groupBy {  inntekt ->
            inntekt.utbetalingsperiode
        }
    }.let {
        Beregningsperiode(it, "§ 8-30 andre ledd - rapportert inntekt (se § 8-29) til a-ordningen etter reglene i a-opplysningsloven de siste tolv kalendermånedene før arbeidsuførheten inntraff (${førsteSykdomsdag}) legges til grunn.")
    }

    return Vurdering.Avklart(beregningsperiode.inntekter
            .flatMap {
                it.value.flatMap {
                    it.value
                }
            }.map {
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
        return Uavklart(KREVER_SKJØNNSMESSIG_VURDERING,
                "§ 8-30 andre ledd - Sykepengegrunnlaget skal fastsettes ved skjønn fordi omregner årsinntekt ($omregnetÅrsinntekt) avviker mer enn 25% (${avvik * 100}%) fra rapportert inntekt ($rapportertInntekt)",
                sammenligningsgrunnlag.grunnlag) // FIXME: hva ønsker vi egentlig som referert grunnlag her ?
    }

    return Vurdering.Avklart(omregnetÅrsinntekt, paragraf_8_30_første_ledd, beregnetAktuellMånedsinntekt.grunnlag, "SPA")
}

data class Beregningsperiode(val inntekter: Map<String, Map<YearMonth, List<Inntekt>>>, val begrunnelse: String)

data class Sykepengegrunnlag(val sykepengegrunnlagNårTrygdenYter: Vurdering.Avklart<Long, Beregningsperiode>, val sykepengegrunnlagIArbeidsgiverperioden: Vurdering.Avklart<Long, Beregningsperiode>)

