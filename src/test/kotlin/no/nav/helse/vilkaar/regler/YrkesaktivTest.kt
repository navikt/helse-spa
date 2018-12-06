package no.nav.helse.vilkaar.regler

import no.nav.helse.Opptjeningstid
import no.nav.helse.Opptjeningstype
import no.nav.helse.Soknad
import no.nav.helse.Sykemelding
import no.nav.nare.evaluation.Evaluation
import no.nav.nare.evaluation.Result
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class YrkesaktivTest {

    @Test
    fun `ingen opptjeningstid er ikke yrkesaktiv`() {
        val evaluation: Evaluation = Yrkesaktiv().evaluate(soknadTemplate)
        assertThat(evaluation.result()).isEqualTo(Result.NO)
    }

    @Test
    fun `åresvis i jobb før en sykemeldning betyr at brukeren er yrkesaktiv`() {
        val soknad: Soknad = soknadTemplate.copy(opptjeningstid = åresvis())
        val evaluation: Evaluation = Yrkesaktiv().evaluate(soknad)
        assertThat(evaluation.result()).isEqualTo(Result.YES)
    }

    @Test
    fun `åresvis i jobb, men med én uke uten opptjening før en sykemeldning betyr at brukeren er ikke yrkesaktiv`() {
        val soknad: Soknad = soknadTemplate.copy(opptjeningstid = åresvis())
        val evaluation: Evaluation = Yrkesaktiv().evaluate(soknad)
        assertThat(evaluation.result()).isEqualTo(Result.YES)
    }

    @Test
    fun `sporadisk opptjening som likevel blir til fire uker før sykemeldning betyr at brukeren er yrkesaktiv`() {
        val soknad: Soknad = soknadTemplate.copy(opptjeningstid = sporadisk())
        val evaluation: Evaluation = Yrkesaktiv().evaluate(soknad)
        assertThat(evaluation.result()).isEqualTo(Result.YES)
    }

    @Test
    fun `sporadisk opptjening skal sees som én periode`() {
        val skalVæreÉnEnkeltPeriode = sporadisk().toList()
                .map { Periode(it.fom, it.tom) }
                .sortedBy { it.fom }
                .smeltSammenTilstøtendePerioder()
        assertThat(skalVæreÉnEnkeltPeriode).hasSize(1)
        assertThat(skalVæreÉnEnkeltPeriode[0].fom).isEqualTo(LocalDate.of(2017, Month.SEPTEMBER, 3))
        assertThat(skalVæreÉnEnkeltPeriode[0].tom).isEqualTo(LocalDate.of(2017, Month.SEPTEMBER, 30))
    }
}

fun sporadisk(): Collection<Opptjeningstid> {
    return listOf(Opptjeningstid(
            fom = LocalDate.of(2017, Month.SEPTEMBER, 3),
            tom = LocalDate.of(2017, Month.SEPTEMBER, 9),
            type = Opptjeningstype.JOBB
    ), Opptjeningstid(
            fom = LocalDate.of(2017, Month.SEPTEMBER, 10),
            tom = LocalDate.of(2017, Month.SEPTEMBER, 16),
            type = Opptjeningstype.JOBB
    ), Opptjeningstid(
            fom = LocalDate.of(2017, Month.SEPTEMBER, 17),
            tom = LocalDate.of(2017, Month.SEPTEMBER, 23),
            type = Opptjeningstype.JOBB
    ), Opptjeningstid(
            fom = LocalDate.of(2017, Month.SEPTEMBER, 24),
            tom = LocalDate.of(2017, Month.SEPTEMBER, 30),
            type = Opptjeningstype.JOBB
    )).shuffled()
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
        norskIdent = "00000000000",
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