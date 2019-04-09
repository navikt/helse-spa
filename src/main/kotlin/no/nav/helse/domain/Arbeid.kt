package no.nav.helse.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class  Arbeidsgiver(val navn: String, val orgnummer: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Arbeidsforhold(val arbeidsgiver: Arbeidsgiver, val startdato: LocalDate, val sluttdato: LocalDate?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsforholdMedInntekter(val arbeidsforhold: Arbeidsforhold)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsforholdWrapper(val arbeidsforhold: Array<ArbeidsforholdMedInntekter>)
