package no.nav.helse.oppslag.arbeidinntektytelse

import arrow.core.Either
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.behandling.Sykepengesøknad
import no.nav.helse.oppslag.AktørId
import no.nav.helse.oppslag.StsRestClient
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidInntektYtelseDTO
import no.nav.helse.streams.defaultObjectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

class ArbeidInntektYtelseOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    private val log = LoggerFactory.getLogger(ArbeidInntektYtelseOppslag::class.java.name)

    fun hentArbeidInntektYtelse(sykepengesøknad: Sykepengesøknad) : Either<Exception, ArbeidInntektYtelseDTO> {
        val forsteSykdomsdag = sykepengesøknad.startSyketilfelle
        // Opptjeningstid = minst 4 uker i arbeid før sykdommen
        val fireUkerForSykdomsDag = forsteSykdomsdag.minus(4, ChronoUnit.WEEKS)

        return hentArbeidsforholdRest(AktørId(sykepengesøknad.aktorId), fireUkerForSykdomsDag, forsteSykdomsdag)
    }

    fun hentArbeidsforholdRest(aktørId: AktørId, fom: LocalDate, tom: LocalDate) : Either<Exception, ArbeidInntektYtelseDTO> {
        val bearer = stsRestClient.token()
        val (_, _, result) =
                "$sparkelUrl/api/arbeidsforhold/${aktørId.aktor}/inntekter?fom=$fom&tom=$tom".httpGet()
                        .header(mapOf(
                                "Authorization" to "Bearer $bearer",
                                "Accept" to "application/json",
                                "Nav-Call-Id" to UUID.randomUUID().toString(),
                                "Nav-Consumer-Id" to "spa"
                        ))
                        .responseString()

        val (_, error) = result

        return error?.exception?.let {
            log.error("Error in arbeidsforhold lookup", it)
            Either.Left(it)
        } ?: try {
            Either.Right(defaultObjectMapper.readValue<ArbeidInntektYtelseDTO>(result.component1()!!))
        } catch (err: Exception) {
            Either.Left(err)
        }
    }
}

