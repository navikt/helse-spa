package no.nav.helse.oppslag.arbeidinntektytelse.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class InfotrygdSakDTO(val iverksatt: LocalDate?,
                           val tema: String,
                           val behandlingstema: String,
                           val opphørerFom: LocalDate?,
                           val ikkeStartet: Boolean)
