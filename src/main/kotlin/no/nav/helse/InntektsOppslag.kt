package no.nav.helse

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.serde.defaultObjectMapper
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class Inntekt(val periode: YearMonth, val beløp: Long, val orgnummer:String)

class InntektOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    private fun hentInntekter(aktorId: String, fom: LocalDate, tom: LocalDate): InntektsOppslagResultat {
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

        return defaultObjectMapper.readValue(result.component1(), InntektsOppslagResultat::class.java)
    }

    fun hentInntekt(aktorId: String, fom: LocalDate, tom: LocalDate) = hentInntekter(aktorId, fom, tom).lagInntektsListe()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InntektsOppslagResultat(val arbeidsInntektIdentListe : Array<ArbeidsInntektIdent>) {
    fun lagInntektsListe() = if (arbeidsInntektIdentListe.size != 1) throw Exception("Forventet kun én ArbeidsInntektIdent") else
        arbeidsInntektIdentListe[0].arbeidsInntektMaaned
                .flatMap { it.arbeidsInntektInformasjon.inntektListe.asIterable() }
                .map { Inntekt(it.utbetaltIPeriode, it.beloep, it.virksomhet.orgnummer) }
}

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
                               val virksomhet: Orgnummer,
                               @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MMz")
                               val utbetaltIPeriode: YearMonth)

data class Orgnummer(val orgnummer:String)
