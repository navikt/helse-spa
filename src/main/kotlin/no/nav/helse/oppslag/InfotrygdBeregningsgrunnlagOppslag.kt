package no.nav.helse.oppslag

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.Either
import no.nav.helse.streams.defaultObjectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class InfotrygdBeregningsgrunnlagOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    private val log = LoggerFactory.getLogger(InfotrygdBeregningsgrunnlagOppslag::class.java.name)

    fun hentInfotrygdBeregningsgrunnlag(aktorId: String, tom: LocalDate): Either<Exception, InfotrygdBeregningsgrunnlag> {
        val bearer = stsRestClient.token()
        val (_, _, result) = "$sparkelUrl/api/infotrygdberegningsgrunnlag/$aktorId?tom=$tom&fom=${tom.minusYears(3)}".httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to UUID.randomUUID().toString(),
                        "Nav-Consumer-Id" to "spa"
                ))
                .responseString()
        val (_, error) = result

        return error?.exception?.let {
            log.error("Error in hentInfotrygdBeregningsgrunnlag lookup", it)
            Either.Left(it)
        } ?: try {
            Either.Right(defaultObjectMapper.readValue(result.component1(), InfotrygdBeregningsgrunnlag::class.java))
        } catch (err: Exception) {
            Either.Left(err)
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InfotrygdBeregningsgrunnlag(
        val paaroerendeSykdomListe : List<PeriodeYtelse>,
        val engangstoenadListe : List<Grunnlag>,
        val sykepengerListe : List<PeriodeYtelse>,
        val foreldrepengerListe : List<PeriodeYtelse>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Behandlingstema(
    val value : String
)

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InfotrygdVedtak(
        val anvistPeriode : Periode,
        val utbetalingsgrad : Int)

enum class InntektsPeriodeVerdi {
    M, F, U, D
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Inntektsperiode(
        val value: InntektsPeriodeVerdi
)

data class Arbeidsforhold(
        val inntektForPerioden : Int,
        val inntektsPeriode : Inntektsperiode,
        val orgnr : String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Grunnlag(
        val identdato : LocalDate,
        val behandlingstema: Behandlingstema, // skal være "SP" for sykepenger
        val periode: Periode,
        val vedtakListe: List<InfotrygdVedtak>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PeriodeYtelse(
        ////// Grunnlag: ///////////
        val identdato : LocalDate,
        val behandlingstema: Behandlingstema, // skal være "SP" for sykepenger
        val periode: Periode,
        val vedtakListe: List<InfotrygdVedtak>,
        /////////////////
        //val arbeidskategori : Arbeidskategori,
        val arbeidsforholdListe : List<Arbeidsforhold>
)
