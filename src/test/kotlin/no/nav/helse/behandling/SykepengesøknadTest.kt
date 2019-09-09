package no.nav.helse.behandling

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.behandling.mvp.readResource
import no.nav.helse.behandling.søknad.Sykepengesøknad
import no.nav.helse.streams.defaultObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykepengesøknadTest {

    @Test
    fun `serialisering av en søknad skal serialisere jsonNode`() {
        val søknadJson = defaultObjectMapper.readTree("/søknader/arbeidstaker_sendt_nav.json".readResource())

        val søknad = Sykepengesøknad(søknadJson)

        assertEquals(søknadJson, defaultObjectMapper.valueToTree(søknad))
    }

    @Test
    fun `deserialisering av en søknad skal deserialisere jsonNode`() {
        val søknad = Sykepengesøknad(defaultObjectMapper.readTree("/søknader/arbeidstaker_sendt_nav.json".readResource()))

        assertEquals("1c6d9930-ce09-49dc-bcb3-b6fee2ff4e32", søknad.id)
    }

    @Test
    fun `kan hente ut arbeidgjenopptatt om den er satt til null`() {
        val søknad = Sykepengesøknad(defaultObjectMapper.readTree("/søknader/arbeidstaker_sendt_nav.json".readResource()))
        assertNull(søknad.arbeidGjenopptatt)
    }

    @Test
    fun `kan hente ut arbeidgjenopptatt om den mangler`() {
        val søknadJson = defaultObjectMapper.readTree("/søknader/arbeidstaker_sendt_nav.json".readResource()) as ObjectNode
        søknadJson.remove("arbeidGjenopptatt")
        val søknad = Sykepengesøknad(søknadJson)
        assertNull(søknad.arbeidGjenopptatt)
    }

    @Test
    fun `kan hente ut arbeidgjenopptatt om den er satt til en verdi`() {
        val søknadJson = defaultObjectMapper.readTree("/søknader/arbeidstaker_sendt_nav.json".readResource()) as ObjectNode
        søknadJson.replace("arbeidGjenopptatt", JsonNodeFactory.instance.textNode("2019-01-01"))
        val søknad = Sykepengesøknad(søknadJson)
        assertEquals(LocalDate.of(2019,1,1), søknad.arbeidGjenopptatt)
    }

    @Test
    fun `korrigert arbeidstid er false om ingen perioder er korrigerte`() {
        val søknad = Sykepengesøknad(defaultObjectMapper.readTree("/søknader/arbeidstaker_sendt_nav.json".readResource()))
        assertFalse(søknad.harKorrigertArbeidstid)
    }

    @Test
    fun `korrigert arbeidstid er true om en perioder har korrigert grad`() {
        val søknadJson = defaultObjectMapper.readTree("/søknader/arbeidstaker_sendt_nav.json".readResource()) as ObjectNode

        val periode = søknadJson["soknadsperioder"][0] as ObjectNode
        periode.replace("faktiskGrad", JsonNodeFactory.instance.numberNode(60.0))
        periode.replace("avtaltTimer", JsonNodeFactory.instance.numberNode(37.5))

        søknadJson.replace("soknadsperioder", JsonNodeFactory.instance.arrayNode().add(periode))

        val søknad = Sykepengesøknad(søknadJson)

        assertTrue(søknad.harKorrigertArbeidstid)
    }

    @Test
    fun `korrigert arbeidstid er true om en perioder har korrigerte timer`() {
        val søknadJson = defaultObjectMapper.readTree("/søknader/arbeidstaker_sendt_nav.json".readResource()) as ObjectNode

        val periode = søknadJson["soknadsperioder"][0] as ObjectNode
        periode.replace("faktiskTimer", JsonNodeFactory.instance.numberNode(20.0))
        periode.replace("avtaltTimer", JsonNodeFactory.instance.numberNode(37.5))

        søknadJson.replace("soknadsperioder", JsonNodeFactory.instance.arrayNode().add(periode))

        val søknad = Sykepengesøknad(søknadJson)

        assertTrue(søknad.harKorrigertArbeidstid)
    }
}
