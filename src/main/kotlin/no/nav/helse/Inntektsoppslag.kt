package no.nav.helse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.serde.defaultObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Inntektsoppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    private fun hentInntekter(aktorId: String, fom: LocalDate, tom: LocalDate, type: String): List<Inntekt> {
        val bearer = stsRestClient.token()

        val dyFom = fom.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        val dyTom = tom.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        val (_, _, result) = "$sparkelUrl/api/inntekt/$aktorId/$type?fom=$dyFom&tom=$dyTom".httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to "anything",
                        "Nav-Consumer-Id" to "spa"
                ))
                .responseString()

        return defaultObjectMapper.readValue(result.component1(), InntektsoppslagResultat::class.java).inntekter
    }

    fun hentBeregningsgrunnlag(aktorId: String, fom: LocalDate, tom: LocalDate) = hentInntekter(aktorId, fom, tom, "beregningsgrunnlag")
    fun hentSammenligningsgrunnlag(aktorId: String, fom: LocalDate, tom: LocalDate) = hentInntekter(aktorId, fom, tom, "sammenligningsgrunnlag")
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InntektsoppslagResultat(val inntekter : List<Inntekt>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Opptjeningsperiode(val fom: LocalDate, val tom: LocalDate)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Inntektsarbeidsgiver(val orgnr: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Inntekt(val arbeidsgiver: Inntektsarbeidsgiver, val opptjeningsperiode: Opptjeningsperiode, val beløp: BigDecimal)
