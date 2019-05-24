package no.nav.helse.oppslag.arbeidinntektytelse.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArenaSakDTO(val tema: String,
                       val fom: LocalDate?,
                       val tom: LocalDate?)
