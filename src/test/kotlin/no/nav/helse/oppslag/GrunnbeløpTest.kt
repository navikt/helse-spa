package no.nav.helse.oppslag

import assertk.assert
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDate.of
import java.time.Month

class GrunnbeløpTest {

    @Test
    fun ` Grunnbeløp skal gjelde for hele den oppgitte periode, inklusiv start- og sluttdato`() {

        val grunnbeløp2017 = Grunnbeløp(of(2017, Month.MAY, 1), of(2018, Month.APRIL, 30), 93634L)
        assert(grunnbeløp2017.gjelderFor(of(2017, Month.APRIL, 30))).isFalse()
        assert(grunnbeløp2017.gjelderFor(of(2017, Month.MAY, 1))).isTrue()
        assert(grunnbeløp2017.gjelderFor(of(2018, Month.APRIL, 30))).isTrue()
        assert(grunnbeløp2017.gjelderFor(of(2018, Month.MAY, 1))).isFalse()
    }

    @Test
    fun ` Nyeste innslag i Grunnbeløpslista må være under et år gammelt `() {

        assert(grunnbeløpListe.maxBy { it.fom }!!.fom.plusYears(1)).isGreaterThan(LocalDate.now())
    }

    @Test
    fun ` Skal returnere grunnbeløp på 96883 for måned mai 2019 `() {

        assert(getGrunnbeløpForDato(of(2019, Month.MAY, 1))).isEqualTo(96883L)
        assert(getGrunnbeløpForDato(of(2019, Month.MAY, 31))).isEqualTo(96883L)
    }

    @Test
    fun ` Skal returnere grunnbeløp på 96883 for måned april 2019 `() {

        assert(getGrunnbeløpForDato(of(2019, Month.APRIL, 1))).isEqualTo(96883L)
        assert(getGrunnbeløpForDato(of(2019, Month.APRIL, 30))).isEqualTo(96883L)
    }

    @Test
    fun ` Skal returnere grunnbeløp på 93634 for måned mars 2018 `() {

        assert(getGrunnbeløpForDato(of(2018, Month.MARCH, 1))).isEqualTo(93634L)
        assert(getGrunnbeløpForDato(of(2018, Month.MARCH, 31))).isEqualTo(93634L)
    }

    @Test
    fun ` Skal returnere grunnbeløp på 92576 for måned mai 2016  `() {

        assert(getGrunnbeløpForDato(of(2016, Month.MAY, 1))).isEqualTo(92576L)
        assert(getGrunnbeløpForDato(of(2016, Month.MAY, 31))).isEqualTo(92576L)
    }

    @Test
    fun ` Skal returnere grunnbeløp på 90068 for måned mars 2015 `() {

        assert(getGrunnbeløpForDato(of(2015, Month.AUGUST, 1))).isEqualTo(90068L)
        assert(getGrunnbeløpForDato(of(2015, Month.AUGUST, 31))).isEqualTo(90068L)
    }

}