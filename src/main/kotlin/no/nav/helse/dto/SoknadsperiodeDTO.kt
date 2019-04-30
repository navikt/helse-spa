package no.nav.helse.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class SoknadsperiodeDTO(val fom: LocalDate,
                             val tom: LocalDate,
                             val sykmeldingsgrad: Int)
