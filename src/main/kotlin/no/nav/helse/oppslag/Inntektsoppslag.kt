package no.nav.helse.oppslag

import arrow.core.Either
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.streams.defaultObjectMapper
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

class Inntektsoppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    private val log = LoggerFactory.getLogger(Inntektsoppslag::class.java.name)

    private fun hentInntekter(aktorId: String, fom: LocalDate, tom: LocalDate, type: String): Either<Exception, List<Inntekt>> {
        val bearer = stsRestClient.token()

        val dyFom = fom.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val dyTom = tom.format(DateTimeFormatter.ofPattern("yyyy-MM"))

        val (_, _, result) = "$sparkelUrl/api/inntekt/$aktorId/$type?fom=$dyFom&tom=$dyTom".httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to UUID.randomUUID().toString(),
                        "Nav-Consumer-Id" to "spa"
                ))
                .responseString()
        val (_, error) = result

        return error?.exception?.let {
            log.error("Error in inntekt lookup", it)
            Either.Left(it)
        } ?: try {
            Either.Right(defaultObjectMapper.readValue(result.component1(), InntektsoppslagResultat::class.java).inntekter)
        } catch (err: Exception) {
            Either.Left(err)
        }
    }

    fun hentBeregningsgrunnlag(aktorId: String, virksomhetsnummer: String, fom: LocalDate, tom: LocalDate) = hentInntekter(aktorId, fom, tom, "beregningsgrunnlag/$virksomhetsnummer")
    fun hentSammenligningsgrunnlag(aktorId: String, fom: LocalDate, tom: LocalDate) = hentInntekter(aktorId, fom, tom, "sammenligningsgrunnlag")
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InntektsoppslagResultat(val inntekter : List<Inntekt>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Inntektsarbeidsgiver(val identifikator: String, val type: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Inntekt(val virksomhet: Inntektsarbeidsgiver, val utbetalingsperiode: YearMonth, val beløp: BigDecimal, val type: String, val ytelse: Boolean, val kode: String?)
