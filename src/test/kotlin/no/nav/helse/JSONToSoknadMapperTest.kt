package no.nav.helse

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.jupiter.api.Test
import java.time.LocalDate

class JSONToSoknadMapperTest {

    @Test
    fun `should parse a simple søknad`() {
        val input = JSONObject(this::class.java.classLoader.getResource("enkel_soknad.json").readText())

        val søknad = JSONToSoknadMapper().apply(input)

        assertThat(søknad.søknadsNr).isEqualTo("alpha-1")

        val sykemeldingFom = LocalDate.of(2017, 1, 1)
        val sykemeldingTom = LocalDate.of(2017, 2, 28)

        assertThat(søknad.sykemelding.fom).isEqualTo(sykemeldingFom)
        assertThat(søknad.sykemelding.tom).isEqualTo(sykemeldingTom)
    }
}