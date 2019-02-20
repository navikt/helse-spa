package no.nav.helse

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.serde.defaultObjectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class InntektOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    private fun hentInntektsliste(aktorId: String, fom: LocalDate, tom: LocalDate): InntektsOppslagResultat {
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

    fun hentInntekt(aktorId: String, fom: LocalDate, tom: LocalDate): Inntektsfakta {
        val registerdata = hentInntektsliste(aktorId, fom, tom)
        return Inntektsfakta(registerdata, fastsettAktuellMånedsinntekt(registerdata) * 12)
    }
}

fun fastsettAktuellMånedsinntekt(registerdata : InntektsOppslagResultat) : Long {
    if (registerdata.arbeidsInntektIdentListe.size != 1) {
        throw Exception("Forventet kun én ArbeidsInntektIdent")
    }

    val måneder = registerdata.arbeidsInntektIdentListe[0].arbeidsInntektMaaned
    if (måneder.size != 12) {
        throw Exception("Forventet 12 måneder")
    }

    return måneder.slice(9..11)
            .map { it.arbeidsInntektInformasjon.inntektListe }
            .map { if (it.size != 1) throw Exception("Forventet kun én raportert inntekt per måned") else it[0] }
            .map { it.beloep }
            .reduce(Long::plus) / 3
}

data class Inntektsfakta(val registerdata : InntektsOppslagResultat,
                         val fastsattÅrsinntekt : Long)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InntektsOppslagResultat(val arbeidsInntektIdentListe : Array<ArbeidsInntektIdent>)

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