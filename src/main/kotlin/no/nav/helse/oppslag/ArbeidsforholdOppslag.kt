package no.nav.helse.oppslag

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.Either
import no.nav.helse.behandling.Sykepengesøknad
import no.nav.helse.domain.Arbeidsforhold
import no.nav.helse.domain.ArbeidsforholdWrapper
import no.nav.helse.map
import no.nav.helse.streams.defaultObjectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

class ArbeidsforholdOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    private val log = LoggerFactory.getLogger(ArbeidsforholdOppslag::class.java.name)

    fun hentArbeidsforhold(sykepengesøknad: Sykepengesøknad) : Either<Exception, List<Arbeidsforhold>> {
        val forsteSykdomsdag = sykepengesøknad.startSyketilfelle
        // Opptjeningstid = minst 4 uker i arbeid før sykdommen
        val fireUkerForSykdomsDag = forsteSykdomsdag.minus(4, ChronoUnit.WEEKS)

        return hentArbeidsforholdRest(AktørId(sykepengesøknad.aktorId), fireUkerForSykdomsDag, forsteSykdomsdag).map {
            it.toList()
        }
    }

    fun hentArbeidsforholdRest(aktørId: AktørId, fom: LocalDate, tom: LocalDate) : Either<Exception, List<Arbeidsforhold>> {
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
            Either.Right(defaultObjectMapper.readValue<ArbeidsforholdWrapper>(result.component1()!!).arbeidsforhold.map {
                it.arbeidsforhold
            })
        } catch (err: Exception) {
            Either.Left(err)
        }
    }
}

