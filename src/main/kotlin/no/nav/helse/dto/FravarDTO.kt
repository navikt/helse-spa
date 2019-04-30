package no.nav.helse.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class FravarDTO(
        val fom: LocalDate,
        val tom: LocalDate,
        val type: FravarstypeDTO
)
