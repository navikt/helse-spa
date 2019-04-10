package no.nav.helse.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

data class ArbeidsgiverFraSøknad(val navn: String, val orgnummer: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Arbeidsgiver(val identifikator: String, val type: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Arbeidsforhold(val type: String, val arbeidsgiver: Arbeidsgiver, val startdato: LocalDate, val sluttdato: LocalDate?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsforholdMedInntekter(val arbeidsforhold: Arbeidsforhold)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsforholdWrapper(val arbeidsforhold: Array<ArbeidsforholdMedInntekter>)
