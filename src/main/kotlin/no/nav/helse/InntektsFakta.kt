package no.nav.helse

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.serde.defaultObjectMapper
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class InntektOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {

    fun hentInntekt(aktorId: String, fom: LocalDate, tom:LocalDate): InntektsFakta {
        val bearer = stsRestClient.token()

        val dyFom = fom.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        val dyTom = tom.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        val (_, _, result) = "$sparkelUrl/api/inntekt/$aktorId?fom=$dyFom&tom=$dyTom".httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to "anything",
                        "Nav-Consumer-Id" to "spa"
                ))
                .responseString()

        return defaultObjectMapper.readValue(result.component1(), InntektsFakta::class.java)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InntektsFakta(val arbeidsInntektIdentListe : Array<ArbeidsInntektIdent>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsInntektIdent(val arbeidsInntektMaaned: Array<ArbeidsInntektMaaned>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsInntektMaaned(val arbeidsInntektInformasjon: ArbeidsInntektInformasjon,
                                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MMz")
                                val aarMaaned:YearMonth)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsInntektInformasjon(val inntektListe: Array<InntektListeElement>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InntektListeElement(val beloep: Long,
                               @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MMz")
                               val utbetaltIPeriode: YearMonth)