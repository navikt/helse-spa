package no.nav.helse.oppslag

import java.time.LocalDate
import java.time.LocalDate.MAX
import java.time.LocalDate.of
import java.time.Month.APRIL
import java.time.Month.MAY

data class Grunnbeløp(
        val fom: LocalDate,
        val tom: LocalDate,
        val verdi: Long
)

fun Grunnbeløp.gjelderFor(dato: LocalDate): Boolean {
    return !(dato.isBefore(this.fom) || dato.isAfter(this.tom))
}

val grunnbeløpListe: Set<Grunnbeløp> = setOf(
        Grunnbeløp(of(2019, MAY, 1), MAX, 99858L),
        Grunnbeløp(of(2018, MAY, 1), of(2019, APRIL, 30), 96883L),
        Grunnbeløp(of(2017, MAY, 1), of(2018, APRIL, 30), 93634L),
        Grunnbeløp(of(2016, MAY, 1), of(2017, APRIL, 30), 92576L),
        Grunnbeløp(of(2015, MAY, 1), of(2016, APRIL, 30), 90068L)
)

fun getGrunnbeløpForDato(dato: LocalDate): Long {
    return grunnbeløpListe.first { it.gjelderFor(dato) }.verdi
}