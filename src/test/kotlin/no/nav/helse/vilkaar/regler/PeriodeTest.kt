package no.nav.helse.vilkaar.regler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class PeriodeTest {

    @Test
    fun `to perioder tilstøter eller overlapper om de dekker noe av samme tid`() {
        val periodeA = Periode(fom = LocalDate.of(2001, Month.JANUARY, 4),
                tom = LocalDate.of(2001, Month.JANUARY, 8))
        val periodeB = Periode(fom = LocalDate.of(2001, Month.JANUARY, 7),
                tom = LocalDate.of(2001, Month.JANUARY, 10))

        assertThat(periodeA.tilstøterEllerOverlapper(periodeB)).isTrue()
        assertThat(periodeB.tilstøterEllerOverlapper(periodeA)).isTrue()
    }

    @Test
    fun `en periode tilstøter eller overlapper seg selv`() {
        val periodeA = Periode(fom = LocalDate.of(2001, Month.MARCH, 4),
                tom = LocalDate.of(2001, Month.MARCH, 4))

        assertThat(periodeA.tilstøterEllerOverlapper(periodeA)).isTrue()
    }

    @Test
    fun `tilstøtende perioder`() {
        val periodeA = Periode(fom = LocalDate.of(2001, Month.APRIL, 4),
                tom = LocalDate.of(2001, Month.APRIL, 30))
        val periodeB = Periode(fom = LocalDate.of(2001, Month.MAY, 1),
                tom = LocalDate.of(2001, Month.MAY, 1))

        assertThat(periodeA.tilstøterEllerOverlapper(periodeB)).isTrue()
        assertThat(periodeB.tilstøterEllerOverlapper(periodeA)).isTrue()
    }

    @Test
    fun `perioder med én dags mellomrom tilstøter eller overlapper ikke`() {
        val periodeA = Periode(fom = LocalDate.of(2001, Month.APRIL, 4),
                tom = LocalDate.of(2001, Month.APRIL, 29))
        val periodeB = Periode(fom = LocalDate.of(2001, Month.MAY, 1),
                tom = LocalDate.of(2001, Month.MAY, 1))

        assertThat(periodeA.tilstøterEllerOverlapper(periodeB)).isFalse()
        assertThat(periodeB.tilstøterEllerOverlapper(periodeA)).isFalse()
    }

    @Test
    fun `to perioder som ligger inntil hverandre skal være mulig å sette sammen til én periode`() {
        val periodeA = Periode(fom = LocalDate.of(2001, Month.APRIL, 4),
                tom = LocalDate.of(2001, Month.APRIL, 30))
        val periodeB = Periode(fom = LocalDate.of(2001, Month.MAY, 1),
                tom = LocalDate.of(2001, Month.MAY, 1))

        val liste = listOf(periodeA, periodeB)
        val lastExpanded = liste.finnSisteUtvidedePeriode()!!

        assertThat(lastExpanded.fom).isEqualTo(periodeA.fom)
        assertThat(lastExpanded.tom).isEqualTo(periodeB.tom)
    }

    @Test
    fun `flere perioder der noen ligger inntil hverandre`() {
        val periodeA = Periode(fom = LocalDate.of(2001, Month.JANUARY, 1),
                tom = LocalDate.of(2001, Month.JANUARY, 31))
        val periodeB = Periode(fom = LocalDate.of(2001, Month.JANUARY, 15),
                tom = LocalDate.of(2001, Month.FEBRUARY, 13))

        val periodeC = Periode(fom = LocalDate.of(2001, Month.APRIL, 4),
                tom = LocalDate.of(2001, Month.APRIL, 6))
        val periodeD = Periode(fom = LocalDate.of(2001, Month.APRIL, 7),
                tom = LocalDate.of(2001, Month.MAY, 1))

        val periodeE = Periode(fom = LocalDate.of(2001, Month.MAY, 4),
                tom = LocalDate.of(2001, Month.MAY, 30))
        val periodeF = Periode(fom = LocalDate.of(2001, Month.MAY, 9),
                tom = LocalDate.of(2001, Month.JUNE, 3))

        val liste = listOf(periodeC, periodeB, periodeA, periodeD, periodeE, periodeF)

        val lastExpanded = liste.finnSisteUtvidedePeriode()!!

        assertThat(lastExpanded.fom).isEqualTo(periodeE.fom)
        assertThat(lastExpanded.tom).isEqualTo(periodeF.tom)
    }

    @Test
    fun `utvid fungerer i begger retninger`() {
        val a = Periode(fom = LocalDate.of(2001, Month.JANUARY, 10),
                tom = LocalDate.of(2001, Month.JANUARY, 15))
        val b = Periode(fom = LocalDate.of(2001, Month.JANUARY, 16),
                tom = LocalDate.of(2001, Month.JANUARY, 20))

        val c = a.utvid(b)

        assertThat(c.fom).isEqualTo(a.fom)
        assertThat(c.tom).isEqualTo(b.tom)
    }

    @Test
    fun `utvid er symmetrisk`() {
        val a = Periode(fom = LocalDate.of(2001, Month.JANUARY, 10),
                                tom = LocalDate.of(2001, Month.JANUARY, 15))
        val b = Periode(fom = LocalDate.of(2001, Month.JANUARY, 16),
                                tom = LocalDate.of(2001, Month.JANUARY, 20))

        assertThat(a.utvid(b)).isEqualTo(b.utvid(a))
    }

    @Test
    fun `utvid virker på en ikke-avsluttet periode`() {
        val a = Periode(fom = LocalDate.of(2001, Month.JANUARY, 10),
                tom = null)
        val b = Periode(fom = LocalDate.of(2001, Month.JANUARY, 16),
                tom = LocalDate.of(2001, Month.JANUARY, 20))

        val c = a.utvid(b)

        assertThat(c.fom).isEqualTo(a.fom)
        assertThat(c.tom).isNull()
    }
}
