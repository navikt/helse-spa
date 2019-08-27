package no.nav.helse.behandling

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.readResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SakskompleksTest {

    companion object {

        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        private val sakskompleksJson = objectMapper.readTree("/sakskompleks/sakskompleks.json".readResource())
        private val sakskompleks = Sakskompleks(sakskompleksJson)
        private val sakskompleksSerialisert = objectMapper.readTree(objectMapper.writeValueAsString(sakskompleks))
    }

    @Test
    fun `serialisering av sakskomplekset skal være lik den deserialiserte json`() {
        assertEquals(sakskompleksJson, sakskompleksSerialisert)
    }

    @Test
    fun `sakskompleks inneholder id og aktørId`() {
        assertEquals("71bd853d-36a1-49df-a34c-6e02cf727awd", sakskompleks.id)
        assertEquals("11987654321", sakskompleks.aktørId)
    }

    @Test
    fun `sakskompleks inneholder en sykmelding`() {
        assertEquals(1, sakskompleks.sykmeldinger.size)
        assertEquals("71bd853d-36a1-49df-a34c-6e02cf727cfa", sakskompleks.sykmeldinger[0].sykmelding.id)
    }

    @Test
    fun `sakskompleks inneholder en søknad`() {
        assertEquals(1, sakskompleks.søknader.size)
        assertEquals("68da259c-ff7f-47cf-8fa0-c348ae95e220", sakskompleks.søknader[0].id)
    }

    @Test
    fun `sakskompleks inneholder en inntektsmelding`() {
        assertEquals(1, sakskompleks.inntektsmeldinger.size)
        assertEquals("guid", sakskompleks.inntektsmeldinger[0].id)
    }
}
