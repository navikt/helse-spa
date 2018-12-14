package no.nav.helse

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.jupiter.api.Test
import java.time.LocalDate

class JSONToSoknadMapperTest {

    @Test
    fun `should deserialize`() {
        val input = JSONObject(this::class.java.classLoader.getResource("enkel_soknad.json").readText())

        val søknad = JSONToSoknadMapper().apply(input)

        assertThat(søknad.id).isEqualTo("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        assertThat(søknad.fom).isEqualTo(LocalDate.of(2018, 8, 28))
    }

    @Test
    fun `should serialize`() {
        val soknad = Soknad("1",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "sendt")
        val json = JSONObject(soknad)

        assertThat(json["id"]).isEqualTo("1")
        assertThat(json["status"]).isEqualTo("sendt")
    }
}