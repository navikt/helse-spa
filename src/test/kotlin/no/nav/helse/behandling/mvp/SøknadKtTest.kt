package no.nav.helse.behandling.mvp

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.behandling.søknad.Sykepengesøknad
import no.nav.helse.streams.defaultObjectMapper
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class SøknadKtTest {

    @Test
    fun `søknad med arbeid gjenopptatt er utenfor MVP`() {
        val søknadJson = defaultObjectMapper.readTree("/søknader/arbeidstaker_sendt_nav.json".readResource()) as ObjectNode
        søknadJson.replace("arbeidGjenopptatt", JsonNodeFactory.instance.textNode("2019-01-21"))

        val feil = sjekkSvarISøknaden(søknad = Sykepengesøknad(søknadJson))
        assertEquals(1, feil.size)
        assertEquals("Søker har opplyst at han/hun har vært tilbake i jobb før tiden",feil[0].beskrivelse)
    }

    @Test
    fun `søknad med korrigert arbeidstid`() {
        val søknadJson = defaultObjectMapper.readTree("/søknader/arbeidstaker_sendt_nav.json".readResource()) as ObjectNode
        val periode = søknadJson["soknadsperioder"][0] as ObjectNode
            periode.replace("faktiskGrad", JsonNodeFactory.instance.numberNode(60.0))
            periode.replace("avtaltTimer", JsonNodeFactory.instance.numberNode(37.5))

        søknadJson.replace("soknadsperioder", JsonNodeFactory.instance.arrayNode().add(periode))

        val feil = sjekkSvarISøknaden(søknad = Sykepengesøknad(søknadJson))
        assertEquals(1, feil.size)
        assertEquals("Søker har opplyst at han/hun har jobbet mer enn en periode i sykmeldingen tilsier",feil[0].beskrivelse)
    }
}

fun String.readResource() =
    object {}.javaClass.getResource(this)?.readText(Charsets.UTF_8) ?: throw RuntimeException("did not find resource <$this>")
