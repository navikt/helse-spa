package no.nav.helse

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import javax.xml.datatype.XMLGregorianCalendar

@JsonIgnoreProperties(ignoreUnknown = true)
data class InntektsFakta(val arbeidsInntektIdentListe : Array<ArbeidsInntektIdent>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsInntektIdent(val arbeidsInntektMaaned: Array<ArbeidsInntektMaaned>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsInntektMaaned(val arbeidsInntektInformasjon: ArbeidsInntektInformasjon,
                                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MMz")
                                val aarMaaned:XMLGregorianCalendar)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsInntektInformasjon(val inntektListe: Array<InntektListeElement>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InntektListeElement(val beloep: Long,
                               @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MMz")
                               val utbetaltIPeriode: XMLGregorianCalendar)