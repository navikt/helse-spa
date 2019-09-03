package no.nav.helse.behandling

import no.nav.helse.enkelSykepengeberegning
import no.nav.helse.sykepenger.beregning.Beregningsresultat
import no.nav.helse.sykepenger.beregning.Dagsats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class VedtaksperiodiseringTest {

    @Test
    fun `én dagsats skal gi én vedtaksperiode`() {
        val dag = LocalDate.of(2019, 1, 1)
        val dagsats = 1000L
        val perioder = beregnVedtaksperioder(enkelSykepengeberegning.copy(
            beregningFraInntektsmelding = Beregningsresultat(dagsatser = listOf(Dagsats(dato = dag, sats = 1000L, skalUtbetales = true)), delresultater = emptyList())
        ))
        assertEquals(1, perioder.size)
        assertEquals(dag, perioder[0].fom)
        assertEquals(dag, perioder[0].tom)
        assertEquals(BigDecimal.valueOf(dagsats), perioder[0].dagsats)
    }

    @Test
    fun `samme dagsats over flere sammenhengende dager skal gi én vedtaksperiode`() {
        val førsteDag = LocalDate.of(2019, 1, 1)
        val dagsats = 1000L
        val perioder = beregnVedtaksperioder(enkelSykepengeberegning.copy(
                beregningFraInntektsmelding = Beregningsresultat(dagsatser = listOf(
                        Dagsats(dato = førsteDag, sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(1), sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(2), sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(3), sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(4), sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(5), sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(6), sats = dagsats, skalUtbetales = true)
                ), delresultater = emptyList())
        ))
        assertEquals(1, perioder.size)
        assertEquals(førsteDag, perioder[0].fom)
        assertEquals(førsteDag.plusDays(6), perioder[0].tom)
        assertEquals(BigDecimal.valueOf(dagsats), perioder[0].dagsats)
    }

    @Test
    fun `endret dagsats skal gi to vedtaksperioder`() {
        val førsteDag = LocalDate.of(2019, 1, 1)
        val dagsats = 1000L
        val perioder = beregnVedtaksperioder(enkelSykepengeberegning.copy(
                beregningFraInntektsmelding = Beregningsresultat(dagsatser = listOf(
                        Dagsats(dato = førsteDag, sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(1), sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(2), sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(3), sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(4), sats = 1500L, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(5), sats = 1500L, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(6), sats = 1500L, skalUtbetales = true)
                ), delresultater = emptyList())
        ))
        assertEquals(2, perioder.size)
        assertEquals(førsteDag, perioder[0].fom)
        assertEquals(førsteDag.plusDays(3), perioder[0].tom)
        assertEquals(BigDecimal.valueOf(dagsats), perioder[0].dagsats)
        assertEquals(førsteDag.plusDays(4), perioder[1].fom)
        assertEquals(førsteDag.plusDays(6), perioder[1].tom)
        assertEquals(BigDecimal.valueOf(1500L), perioder[1].dagsats)
    }

    @Test
    fun `uordnede dagsatser skal ikke påvirke resultatet`() {
        val førsteDag = LocalDate.of(2019, 1, 1)
        val dagsats = 1000L
        val perioder = beregnVedtaksperioder(enkelSykepengeberegning.copy(
                beregningFraInntektsmelding = Beregningsresultat(dagsatser = listOf(
                        Dagsats(dato = førsteDag, sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(1), sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(6), sats = 1500L, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(3), sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(2), sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(5), sats = 1500L, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(4), sats = 1500L, skalUtbetales = true)
                ), delresultater = emptyList())
        ))
        assertEquals(2, perioder.size)
        assertEquals(førsteDag, perioder[0].fom)
        assertEquals(førsteDag.plusDays(3), perioder[0].tom)
        assertEquals(BigDecimal.valueOf(dagsats), perioder[0].dagsats)
        assertEquals(førsteDag.plusDays(4), perioder[1].fom)
        assertEquals(førsteDag.plusDays(6), perioder[1].tom)
        assertEquals(BigDecimal.valueOf(1500L), perioder[1].dagsats)
    }


    @Test
    fun `ny dagsats hver dag skal fungere fint`() {
        val førsteDag = LocalDate.of(2019, 1, 1)
        val dagsats = 1000L
        val perioder = beregnVedtaksperioder(enkelSykepengeberegning.copy(
                beregningFraInntektsmelding = Beregningsresultat(dagsatser = listOf(
                        Dagsats(dato = førsteDag, sats = 1500L, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(1), sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(2), sats = 1500L, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(3), sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(4), sats = 1500L, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(5), sats = dagsats, skalUtbetales = true),
                        Dagsats(dato = førsteDag.plusDays(6), sats = 1500L, skalUtbetales = true)
                ), delresultater = emptyList())
        ))
        assertEquals(7, perioder.size)
        (0..6).forEach {
            assertEquals(førsteDag.plusDays(it.toLong()), perioder[it].fom)
            assertEquals(førsteDag.plusDays(it.toLong()), perioder[it].tom)
            when (it % 2) {
                1 -> {
                    assertEquals(BigDecimal.valueOf(dagsats), perioder[it].dagsats)
                }
                0 -> {
                    assertEquals(BigDecimal.valueOf(1500L), perioder[it].dagsats)
                }
            }
        }
    }
}

