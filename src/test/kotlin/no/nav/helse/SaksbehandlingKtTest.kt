package no.nav.helse

import arrow.core.getOrHandle
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


internal class SaksbehandlingKtTest {

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Test
    fun `sakskompleks med 1 arbeidstakersøknad er ok`() {
        val sakskompleksJson = objectMapper.readTree("/sakskompleks/sakskompleks.json".readResource()) as ObjectNode
        val sakskompleks = no.nav.helse.behandling.Sakskompleks(sakskompleksJson)

        val mvpFilter = sakskompleks.mvpFilter()

        assertTrue(mvpFilter.isRight())

        val sak = mvpFilter.getOrHandle { throw RuntimeException() }
        assertEquals("71bd853d-36a1-49df-a34c-6e02cf727awd", sak.id)
    }

    @Test
    fun `sakskompleks med flere arbeidstakersøknader er utafor MVP`() {
        val sakskompleksJson = objectMapper.readTree("/sakskompleks/sakskompleks.json".readResource()) as ObjectNode
        val søknader = sakskompleksJson["søknader"] as ArrayNode
        søknader.add(sakskompleksJson["søknader"][0])
        val sakskompleks = no.nav.helse.behandling.Sakskompleks(sakskompleksJson)
        val mvpFilter = sakskompleks.mvpFilter()

        assertTrue(mvpFilter.isLeft())
        mvpFilter.mapLeft { left ->
            assertEquals(
                "Et sakskompleks må inneholde nøyaktig en søknad med typen ARBEIDSTAKERE",
                left.mvpFeil[0].beskrivelse
            )
        }
    }

    @Test
    fun `sakskompleks med andre søknadstyper er utafor MVP`() {
        val sakskompleksJson = objectMapper.readTree("/sakskompleks/sakskompleks.json".readResource()) as ObjectNode
        val søknad = sakskompleksJson["søknader"][0] as ObjectNode
        søknad.replace("type", JsonNodeFactory.instance.textNode("FRILANSER"))

        val sakskompleks = no.nav.helse.behandling.Sakskompleks(sakskompleksJson)
        val mvpFilter = sakskompleks.mvpFilter()

        assertTrue(mvpFilter.isLeft())
        mvpFilter.mapLeft { left ->
            assertEquals(
                "Et sakskompleks må inneholde nøyaktig en søknad med typen ARBEIDSTAKERE",
                left.mvpFeil[0].beskrivelse
            )
        }
    }
}
