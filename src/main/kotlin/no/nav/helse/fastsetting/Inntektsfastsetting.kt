package no.nav.helse.fastsetting

import no.nav.helse.Arbeidsgiver
import no.nav.helse.Inntekt
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

fun LocalDate.yearMonth() = YearMonth.of(year, month.value)

fun fastsettingAvSykepengegrunnlaget(førsteSykdomsdag: LocalDate, arbeidsgiver: Arbeidsgiver, beregningsgrunnlag: List<Inntekt>, sammenligningsgrunnlag: List<Inntekt>): Vurdering<Sykepengegrunnlag, List<FastsattBeregningsperiode>> {
    val sykepengegrunnlagIArbeidsgiverperioden = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag,
            arbeidsgiver, beregningsgrunnlag)

    if (sykepengegrunnlagIArbeidsgiverperioden is Vurdering.Uavklart) {
        return sykepengegrunnlagIArbeidsgiverperioden
    }

    val fastsattSammenligningsgrunnlag = fastsettSammenligningsgrunnlag(førsteSykdomsdag, sammenligningsgrunnlag)

    if (fastsattSammenligningsgrunnlag is Vurdering.Uavklart) {
        return fastsattSammenligningsgrunnlag
    }

    val sykepengegrunnlagNårTrydenYter = fastsettingAvSykepengegrunnlagetNårTrygdenYterSykepenger(fastsattSammenligningsgrunnlag as Vurdering.Avklart, sykepengegrunnlagIArbeidsgiverperioden as Vurdering.Avklart)

    if (sykepengegrunnlagNårTrydenYter is Vurdering.Uavklart) {
        return sykepengegrunnlagNårTrydenYter
    }

    return Vurdering.Avklart(Sykepengegrunnlag(sykepengegrunnlagNårTrydenYter as Vurdering.Avklart, sykepengegrunnlagIArbeidsgiverperioden), "", emptyList(), "SPA")
}

// https://lovdata.no/lov/1997-02-28-19/§8-28
fun fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag: LocalDate, arbeidsgiver: Arbeidsgiver, inntekter: List<Inntekt>): Vurdering<Long, List<FastsattBeregningsperiode>> {
    val enMånedFør = førsteSykdomsdag.minusMonths(1)
            .yearMonth()
    val treMånederFør = førsteSykdomsdag.minusMonths(3)
            .yearMonth()

    val beregningsperiode = inntekter.filter { inntekt ->
        inntekt.opptjeningsperiode.fom in treMånederFør.atDay(1)..enMånedFør.atEndOfMonth()
                && inntekt.opptjeningsperiode.tom in treMånederFør.atDay(1)..enMånedFør.atEndOfMonth()
    }.map {
        FastsattBeregningsperiode(it, "§ 8-28 tredje ledd bokstav a) – De tre siste kalendermånedene før arbeidstakeren ble arbeidsufør (${førsteSykdomsdag}) legges til grunn.")
    }

    // TODO: sjekke om listen inneholder mer enn tre elementer? (hva om det er rapportert inn to inntekter for en måned?)

    return if (beregningsperiode.isEmpty()) {
        Vurdering.Uavklart(Vurdering.Uavklart.Årsak.MANGELFULL_DATAGRUNNLAG, "Kan ikke avklare sykepengegrunnlaget fordi det ikke er inntekter i beregningsperioden", beregningsperiode)
    } else if (beregningsperiode.any { it.inntekt.arbeidsgiver.orgnr != arbeidsgiver.orgnummer }) {
        Vurdering.Uavklart(Vurdering.Uavklart.Årsak.KREVER_SKJØNNSMESSIG_VURDERING, "Kan ikke avklare sykepengegrunnlaget fordi det andre inntekter i arbeidsgiverperioden enn i fra aktuell arbeidsgiver", beregningsperiode)
    } else if (beregningsperiode.size > 3) {
        Vurdering.Uavklart(Vurdering.Uavklart.Årsak.DÅRLIG_DATAGRUNNLAG, "Kan ikke avklare sykepengegrunnlaget fordi det er ${beregningsperiode.size} inntekter i beregningsperioden, vi forventer tre eller færre.", beregningsperiode)
    } else {
        // § 8-28 andre ledd
        val aktuellMånedsinntekt = beregningsperiode.sumBy { periode ->
            periode.inntekt.beløp.toInt()
        } / beregningsperiode.size

        Vurdering.Avklart(aktuellMånedsinntekt.toLong(), "§ 8-28 andre ledd", beregningsperiode, "spa")
    }
}

fun fastsettSammenligningsgrunnlag(førsteSykdomsdag: LocalDate, sammenligningsgrunnlag: List<Inntekt>) : Vurdering<Long, List<FastsattBeregningsperiode>> {
    val enMånedFør = førsteSykdomsdag.minusMonths(1)
            .yearMonth()
    val tolvMånederFør = førsteSykdomsdag.minusMonths(12)
            .yearMonth()

    val beregningsperiode = sammenligningsgrunnlag.filter { inntekt ->
        inntekt.opptjeningsperiode.fom in tolvMånederFør.atDay(1)..enMånedFør.atEndOfMonth()
                && inntekt.opptjeningsperiode.tom in tolvMånederFør.atDay(1)..enMånedFør.atEndOfMonth()
    }.map {
        FastsattBeregningsperiode(it, "§ 8-30 andre ledd – rapportert inntekt (se § 8-29) til a-ordningen etter reglene i a-opplysningsloven de siste tolv kalendermånedene før arbeidsuførheten inntraff (${førsteSykdomsdag}) legges til grunn.")
    }

    return Vurdering.Avklart(beregningsperiode
            .map {
                it.inntekt.beløp
            }.reduce(BigDecimal::add).longValueExact(), "§ 8-30 andre ledd", beregningsperiode, "spa")
}

// § 8-30 første ledd
fun fastsettingAvSykepengegrunnlagetNårTrygdenYterSykepenger(sammenligningsgrunnlag: Vurdering.Avklart<Long, List<FastsattBeregningsperiode>>,
                                                             beregnetAktuellMånedsinntekt: Vurdering.Avklart<Long, List<FastsattBeregningsperiode>>): Vurdering<Long, List<FastsattBeregningsperiode>> {
    val omregnetÅrsinntekt = beregnetAktuellMånedsinntekt.fastsattVerdi * 12

    val rapportertInntekt = sammenligningsgrunnlag.fastsattVerdi
    val avvik = Math.abs(omregnetÅrsinntekt - rapportertInntekt) / rapportertInntekt.toDouble()

    if (avvik > 0.25) {
        return Vurdering.Uavklart(Vurdering.Uavklart.Årsak.KREVER_SKJØNNSMESSIG_VURDERING,
                "§ 8-30 andre ledd - Sykepengegrunnlaget skal fastsettes ved skjønn fordi omregner årsinntekt ($omregnetÅrsinntekt) avviker mer enn 25% (${avvik * 100}%) fra rapportert inntekt ($rapportertInntekt)",
                sammenligningsgrunnlag.grunnlag) // FIXME: hva ønsker vi egentlig som referert grunnlag her ?
    }

    return Vurdering.Avklart(omregnetÅrsinntekt, "§ 8-30 første ledd", beregnetAktuellMånedsinntekt.grunnlag, "spa")
}

data class FastsattBeregningsperiode(val inntekt: Inntekt, val begrunnelse: String)

data class Sykepengegrunnlag(val sykepengegrunnlagNårTrydenYter: Vurdering.Avklart<Long, List<FastsattBeregningsperiode>>, val sykepengegrunnlagIArbeidsgiverperioden: Vurdering.Avklart<Long, List<FastsattBeregningsperiode>>)

