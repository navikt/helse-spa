package no.nav.helse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.oppslag.SykepengerPeriode
import no.nav.helse.streams.defaultObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class SykepengelisteTest {

    @Test
    fun `should parse two items`() {
        val vedtak: Collection<SykepengerPeriode> = defaultObjectMapper.readValue(toSykepengerVedtak)
        assertThat(vedtak).hasSize(2)
        assertThat(vedtak).contains(
                SykepengerPeriode(fom = LocalDate.of(2010, 1, 1), tom = LocalDate.of(2010, 1, 31), grad = 80.5f, dagsats = BigDecimal.valueOf(213.02)),
                SykepengerPeriode(fom = LocalDate.of(2010, 2, 1), tom = LocalDate.of(2010, 2, 28), grad = 100f, dagsats = BigDecimal.valueOf(1000))
        )
    }

}

val toSykepengerVedtak = """
[
    {
        "fom":"2010-01-01",
        "tom":"2010-01-31",
        "grad": 80.5,
        "dagsats": 213.02
    },{
        "fom":"2010-02-01",
        "tom":"2010-02-28",
        "grad": 100,
        "dagsats": 1000
    }
]
""".trimIndent()