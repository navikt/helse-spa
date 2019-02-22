package no.nav.helse

import java.time.LocalDate
import java.time.YearMonth

fun LocalDate.yearMonth() = YearMonth.of(year, month.value)


// https://lovdata.no/lov/1997-02-28-19/§8-28
fun fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag: LocalDate, inntekter: List<Inntekt>): Vurdering<Sykepengegrunnlag, List<FastsattBeregningsperiode>> {
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
    } else if (beregningsperiode.size > 3) {
        Vurdering.Uavklart(Vurdering.Uavklart.Arsak.DARLIG_DATA, "Kan ikke avklare sykepengegrunnlaget fordi det er ${beregningsperiode.size} inntekter i beregningsperioden, vi forventer tre eller færre.", beregningsperiode)
    } else {
        // § 8-28 andre ledd
        val aktuellMånedsinntekt = beregningsperiode.sumBy { periode ->
            periode.inntekt.beløp.toInt()
        } / beregningsperiode.size

        Vurdering.Avklart(Sykepengegrunnlag(aktuellMånedsinntekt, beregningsperiode), "§ 8-28 andre ledd", beregningsperiode, "spa")
    }
}

// § 8-30 første ledd
fun fastsettingAvSykepengegrunnlagetNårTrygdenYterSykepenger(førsteSykdomsdag: LocalDate, inntekter: List<Inntekt>): Vurdering<Sykepengegrunnlag, List<FastsattBeregningsperiode>>  {
    val beregnetAktuellMånedsinntekt = fastsettingAvSykepengegrunnlagetIArbeidsgiverperioden(førsteSykdomsdag, inntekter)
    if (beregnetAktuellMånedsinntekt is Vurdering.Uavklart) {
        return beregnetAktuellMånedsinntekt
    }

    val avklartSykepengegrunnlagIArbeidsgiverperioden = (beregnetAktuellMånedsinntekt as Vurdering.Avklart)
    val omregnetÅrsinntekt = avklartSykepengegrunnlagIArbeidsgiverperioden.fastsattVerdi.aktuellMånedsinntekt * 12

    return Vurdering.Avklart(Sykepengegrunnlag(omregnetÅrsinntekt, avklartSykepengegrunnlagIArbeidsgiverperioden.fastsattVerdi.beregningsperiode),
            "§ 8-30 første ledd", avklartSykepengegrunnlagIArbeidsgiverperioden.fastsattVerdi.beregningsperiode, "spa")
}

data class FastsattBeregningsperiode(val inntekt: Inntekt, val begrunnelse: String)

data class Sykepengegrunnlag(val aktuellMånedsinntekt: Int, val beregningsperiode: List<FastsattBeregningsperiode>)
