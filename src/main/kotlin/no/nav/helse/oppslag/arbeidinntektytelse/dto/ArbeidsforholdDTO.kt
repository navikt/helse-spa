package no.nav.helse.oppslag.arbeidinntektytelse.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsforholdDTO(val type: String,
                             val arbeidsgiver: ArbeidsgiverDTO,
                             val startdato: LocalDate,
                             val sluttdato: LocalDate? = null)
