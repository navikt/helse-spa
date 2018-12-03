package no.nav.helse

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class SoknadTest {

    @Test
    fun `en yrkesaktiv skal få et utbetalingsvedtak`() {
        val vedtak: Vedtak = soknadTemplate.copy(opptjeningstid = åresvis()).evaluer()
        assertThat(vedtak).isInstanceOf(UtbetalingsVedtak::class.java)
    }

    @Test
    fun `en bruker helt uten opptjening skal få avslag`() {
        val vedtak: Vedtak = soknadTemplate.copy(opptjeningstid = emptyList()).evaluer()
        assertThat(vedtak).isInstanceOf(Avslagsvedtak::class.java)
    }
}

fun åresvis(): Collection<Opptjeningstid> {
    return listOf(Opptjeningstid(
            fom = LocalDate.of(2017, Month.JANUARY, 1),
            tom = LocalDate.of(2017, Month.SEPTEMBER, 30),
            type = Opptjeningstype.JOBB
    ), Opptjeningstid(
            fom = LocalDate.of(2016, Month.JANUARY, 1),
            tom = LocalDate.of(2016, Month.DECEMBER, 31),
            type = Opptjeningstype.JOBB
    ), Opptjeningstid(
            fom = LocalDate.of(2015, Month.JANUARY, 1),
            tom = LocalDate.of(2015, Month.DECEMBER, 31),
            type = Opptjeningstype.JOBB
    ), Opptjeningstid(
            fom = LocalDate.of(2014, Month.JANUARY, 1),
            tom = LocalDate.of(2014, Month.DECEMBER, 31),
            type = Opptjeningstype.JOBB
    ), Opptjeningstid(
            fom = LocalDate.of(2013, Month.JANUARY, 1),
            tom = LocalDate.of(2013, Month.DECEMBER, 31),
            type = Opptjeningstype.JOBB
    )
    ).shuffled()
}

val soknadTemplate = Soknad(
        søknadsNr = "1",
        bruker = "abcd",
        arbeidsgiver = "et orgnr",
        sykemeldingId = "2",
        sykemelding = Sykemelding(
                grad = 0.5f,
                fom = LocalDate.of(2017, Month.OCTOBER, 1),
                tom = LocalDate.of(2017, Month.OCTOBER, 31)
        ),
        korrigertArbeidstid = emptyList(),
        fravær = emptyList(),
        utdanningsgrad = 0,
        søktOmUtenlandsopphold = false,
        annetSykefravær = emptyList(),
        andreInntektskilder = emptyList(),
        opptjeningstid = emptyList()
)