package no.nav.helse.vilkaar.regler

import no.nav.helse.Opptjeningstype
import no.nav.helse.Soknad
import no.nav.nare.evaluation.Evaluation
import no.nav.nare.specifications.AbstractSpecification
import java.time.LocalDate
import java.time.Period

/**
 * en bruker har vært yrkesaktiv om vedkommende har hatt fire sammenhengende uker i arbeid før sykemeldingens start
 */
class Yrkesaktiv : AbstractSpecification<Soknad>() {
    override fun evaluate(t: Soknad): Evaluation {

        // 27 dager, siden det er greit å starte i jobb på dag 28 før sykemelding
        val kritiskPeriode: Period = Period.ofWeeks(4).minusDays(1)
        val kritiskDag: LocalDate = t.sykemelding.fom.minus(kritiskPeriode)
        val sistePeriodeFørSykemelding: Periode? = t.opptjeningstid
                .filter { it.type.equals(Opptjeningstype.JOBB) }
                .filter { !it.tom.isBefore(kritiskDag) }
                .map { Periode(it.fom, it.tom) }
                .finnSisteUtvidedePeriode()

        val opptjeningenErMinstFireUker: Boolean = sistePeriodeFørSykemelding?.fom?.isBefore(kritiskDag) ?: false

        return when (opptjeningenErMinstFireUker) {
            true -> ja("Brukeren har minst fire uker i jobb rett før sykemeldingens start.")
            false -> nei("Brukeren har ikke fire uker i jobb rett fær sykemeldinengs start.")
        }
    }
}

data class Periode(val fom: LocalDate, val tom: LocalDate) {
    fun utvid(that: Periode): Periode {
        return Periode(fom = if (this.fom.isBefore(that.fom)) this.fom else that.fom,
                tom = if (this.tom.isAfter(that.tom)) this.tom else that.tom)
    }

    /**
     * Eg. January 2nd to January 4th ´tilstøterEllerOverlapper´ january 5th to january 7th.
     */
    fun tilstøterEllerOverlapper(that: Periode): Boolean {
        return (this.fom.minusDays(2).isBefore(that.tom))
                && (this.tom.plusDays(2).isAfter(that.fom))
    }

}

/**
 * "Siste" betyr effektivt perioden som starter sist, siden vi slår sammen
 * overlappende eller tilstøtende perioder.
 */
fun Collection<Periode>.finnSisteUtvidedePeriode(): Periode? = if (this.isEmpty()) {
    null
} else sortedBy { periode -> periode.fom }.smeltSammenTilstøtendePerioder().last()

/**
 * assumes that we are sorted on Periode.fom
 */
fun List<Periode>.smeltSammenTilstøtendePerioder(): List<Periode> {
    if (this.isEmpty()) return emptyList()
    if (this.size == 1) return this

    val head: Periode = first()
    val neck: Periode = elementAt(1)
    val firstNonAbutingDay: LocalDate = head.tom.plusDays(2)
    val tail: List<Periode> = this.drop(1)

    if (firstNonAbutingDay.isAfter(neck.fom)) return listOf(head.utvid(neck)).plus(tail.drop(1)).smeltSammenTilstøtendePerioder()

    return listOf(head).plus(tail.smeltSammenTilstøtendePerioder())
}