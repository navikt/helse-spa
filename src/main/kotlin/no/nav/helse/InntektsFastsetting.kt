package no.nav.helse

import no.nav.nare.core.evaluations.Evaluering
import java.time.LocalDate
import java.time.YearMonth

fun LocalDate.yearMonth() = YearMonth.of(year, month.value)


// https://lovdata.no/lov/1997-02-28-19/§8-28
fun fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag: LocalDate, arbeidsgiver: Arbeidsgiver, inntekter: List<Inntekt>): Vurdering<Sykepengegrunnlag, List<FastsattBeregningsperiode>> {
    val enMånedFør = førsteSykdomsdag.minusMonths(1)
            .yearMonth()
    val treMånederFør = førsteSykdomsdag.minusMonths(3)
            .yearMonth()

    val beregningsperiode = inntekter.filter { inntekt ->
        inntekt.periode in treMånederFør..enMånedFør
    }.map {
        FastsattBeregningsperiode(it, "§ 8-28 tredje ledd bokstav a) – De tre siste kalendermånedene før arbeidstakeren ble arbeidsufør (${førsteSykdomsdag}) legges til grunn.")
    }

    // TODO: sjekke om listen inneholder mer enn tre elementer? (hva om det er rapportert inn to inntekter for en måned?)

    return if (beregningsperiode.isEmpty()) {
        Vurdering.Uavklart(Vurdering.Uavklart.Arsak.MANGLENDE_DATA, "Kan ikke avklare sykepengegrunnlaget fordi det ikke er inntekter i beregningsperioden", beregningsperiode)
    } else if (beregningsperiode.any { ! it.inntekt.orgnummer.equals(arbeidsgiver.orgnummer) }) {
        Vurdering.Uavklart(Vurdering.Uavklart.Arsak.SKJONN, "Kan ikke avklare sykepengegrunnlaget fordi det andre inntekter i arbeidsgiverperioden enn i fra aktuell arbeidsgiver", beregningsperiode)
    } else if (beregningsperiode.size > 3) {
        Vurdering.Uavklart(Vurdering.Uavklart.Arsak.DARLIG_DATA, "Kan ikke avklare sykepengegrunnlaget fordi det er ${beregningsperiode.size} inntekter i beregningsperioden, vi forventer tre eller færre.", beregningsperiode)
    } else {
        // § 8-28 andre ledd
        val aktuellMånedsinntekt = beregningsperiode.sumBy { periode ->
            periode.inntekt.beløp.toInt()
        } / beregningsperiode.size

        Vurdering.Avklart(Sykepengegrunnlag(aktuellMånedsinntekt.toLong(), beregningsperiode), "§ 8-28 andre ledd", beregningsperiode, "spa")
    }
}

fun fastsettSammenligningsgrunnlag(førsteSykdomsdag: LocalDate, inntekter: List<Inntekt>) : Vurdering<Sammenligningsgrunnlag, List<FastsattBeregningsperiode>> {
    val enMånedFør = førsteSykdomsdag.minusMonths(1)
            .yearMonth()
    val tolvMånederFør = førsteSykdomsdag.minusMonths(12)
            .yearMonth()

    val beregningsperiode = inntekter.filter { inntekt ->
        inntekt.periode in tolvMånederFør..enMånedFør
    }.map {
        FastsattBeregningsperiode(it, "§ 8-30 andre ledd – rapportert inntekt (se § 8-29) til a-ordningen etter reglene i a-opplysningsloven de siste tolv kalendermånedene før arbeidsuførheten inntraff (${førsteSykdomsdag}) legges til grunn.")
    }

    return Vurdering.Avklart(Sammenligningsgrunnlag(beregningsperiode.map { it.inntekt.beløp }.reduce(Long::plus), beregningsperiode), "§ 8-30 andre ledd", beregningsperiode, "spa")
}

// § 8-30 første ledd
fun fastsettingAvSykepengegrunnlagetNårTrygdenYterSykepenger(førsteSykdomsdag: LocalDate, arbeidsgiver: Arbeidsgiver, inntekter: List<Inntekt>): Vurdering<FastsattSykepengegrunnlag, List<FastsattBeregningsperiode>>  {
    val beregnetAktuellMånedsinntekt = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, arbeidsgiver, inntekter)
    if (beregnetAktuellMånedsinntekt is Vurdering.Uavklart) {
        return beregnetAktuellMånedsinntekt
    }

    val avklartSykepengegrunnlagIArbeidsgiverperioden = (beregnetAktuellMånedsinntekt as Vurdering.Avklart)
    val omregnetÅrsinntekt = avklartSykepengegrunnlagIArbeidsgiverperioden.fastsattVerdi.aktuellMånedsinntekt * 12

    val sammenligningsgrunnlag = fastsettSammenligningsgrunnlag(førsteSykdomsdag, inntekter)
    if (sammenligningsgrunnlag is Vurdering.Uavklart) {
        return sammenligningsgrunnlag
    }

    val rapportertInntekt = (sammenligningsgrunnlag as Vurdering.Avklart).fastsattVerdi.rapportertInntektSiste12Måneder
    val avvik = Math.abs(omregnetÅrsinntekt - rapportertInntekt) / rapportertInntekt.toDouble()

    if (avvik > 0.25) {
        return Vurdering.Uavklart(Vurdering.Uavklart.Arsak.SKJONN,
                "§ 8-30 andre ledd - Sykepengegrunnlaget skal fastsettes ved skjønn fordi omregner årsinntekt ($omregnetÅrsinntekt) avviker mer enn 25% (${avvik * 100}%) fra rapportert inntekt ($rapportertInntekt)",
                sammenligningsgrunnlag.grunnlag) // FIXME: hva ønsker vi egentlig som referert grunnlag her ?
    }

    return Vurdering.Avklart(FastsattSykepengegrunnlag(omregnetÅrsinntekt, avklartSykepengegrunnlagIArbeidsgiverperioden.fastsattVerdi.beregningsperiode),
            "§ 8-30 første ledd", avklartSykepengegrunnlagIArbeidsgiverperioden.fastsattVerdi.beregningsperiode, "spa")
}

data class FastsattBeregningsperiode(val inntekt: Inntekt, val begrunnelse: String)

data class Sykepengegrunnlag(val aktuellMånedsinntekt: Long, val beregningsperiode: List<FastsattBeregningsperiode>)

data class FastsattSykepengegrunnlag(val fastsattÅrsinntektSomSykepengegrunnlag: Long, val beregningsperiode: List<FastsattBeregningsperiode>)

data class Sammenligningsgrunnlag(val rapportertInntektSiste12Måneder: Long, val beregningsperiode: List<FastsattBeregningsperiode>)
