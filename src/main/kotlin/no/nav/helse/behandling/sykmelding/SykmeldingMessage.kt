package no.nav.helse.behandling.sykmelding

import com.fasterxml.jackson.databind.JsonNode

data class SykmeldingMessage(private val jsonNode: JsonNode) {

    val sykmelding get() = Sykmelding(jsonNode["sykmelding"])
}
