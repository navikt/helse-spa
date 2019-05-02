package no.nav.helse.oppslag.arbeidinntektytelse.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class InntektMedArbeidsforholdDTO(val inntekt: InntektDTO, val muligeArbeidsforhold: List<ArbeidsforholdDTO>)
