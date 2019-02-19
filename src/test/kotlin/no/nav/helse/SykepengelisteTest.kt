package no.nav.helse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.serde.defaultObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class SykepengelisteTest {

    @Test
    fun `should parse two items`() {
        val vedtak: Collection<SykepengerVedtak> = defaultObjectMapper.readValue(toSykepengerVedtak)
        assertThat(vedtak).hasSize(2)
        assertThat(vedtak).contains(
                SykepengerVedtak(fom = LocalDate.of(2010, 1, 1), tom = LocalDate.of(2010, 1, 31), grad = 80.5f, mottaker = "I literally don't know", beløp = BigDecimal.valueOf(213.02)),
                SykepengerVedtak(fom = LocalDate.of(2010, 2, 1), tom = LocalDate.of(2010, 2, 28), grad = 100f, mottaker = "I literally don't know", beløp = BigDecimal.valueOf(1000))
        )
    }

}

val toSykepengerVedtak = """
[
    {
        "fom":"2010-01-01",
        "tom":"2010-01-31",
        "grad": 80.5,
        "mottaker": "I literally don't know",
        "beløp": 213.02
    },{
        "fom":"2010-02-01",
        "tom":"2010-02-28",
        "grad": 100,
        "mottaker": "I literally don't know",
        "beløp": 1000
    }
]
""".trimIndent()