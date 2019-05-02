package no.nav.helse.oppslag.arbeidinntektytelse.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsgiverDTO(val identifikator: String, val type: String)
