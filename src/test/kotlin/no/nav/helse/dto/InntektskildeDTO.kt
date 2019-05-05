package no.nav.helse.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class InntektskildeDTO(
        val type: String,
        val sykemeldt: Boolean
)
