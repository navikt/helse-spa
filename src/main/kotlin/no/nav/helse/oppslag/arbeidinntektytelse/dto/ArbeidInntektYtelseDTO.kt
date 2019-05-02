package no.nav.helse.oppslag.arbeidinntektytelse.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidInntektYtelseDTO(val arbeidsforhold: List<ArbeidsforholdDTO>,
                                  val inntekter: List<InntektMedArbeidsforholdDTO>,
                                  val ytelser: List<YtelseDTO>)
