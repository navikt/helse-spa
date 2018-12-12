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

        assertThat(søknad.id).isEqualTo("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        assertThat(søknad.fom).isEqualTo(LocalDate.of(2018, 8, 28))
    }
}