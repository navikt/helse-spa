package no.nav.helse.oppslag.arbeidinntektytelse

import arrow.core.Try
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.behandling.Sakskompleks
import no.nav.helse.oppslag.AktørId
import no.nav.helse.oppslag.StsRestClient
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidInntektYtelseDTO
import no.nav.helse.streams.defaultObjectMapper
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

class ArbeidInntektYtelseOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {

    fun hentArbeidInntektYtelse(sakskompleks: Sakskompleks) : Try<ArbeidInntektYtelseDTO> {
        val forsteSykdomsdag = sakskompleks.startSyketilfelle
        // Opptjeningstid = minst 4 uker i arbeid før sykdommen
        val fireUkerForSykdomsDag = forsteSykdomsdag.minus(4, ChronoUnit.WEEKS)

        return hentArbeidsforholdRest(AktørId(sakskompleks.aktørId), fireUkerForSykdomsDag, forsteSykdomsdag)
    }

    fun hentArbeidsforholdRest(aktørId: AktørId, fom: LocalDate, tom: LocalDate) : Try<ArbeidInntektYtelseDTO> {
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

        return Try {
            error?.exception?.let {
                throw it
            }

            defaultObjectMapper.readValue<ArbeidInntektYtelseDTO>(result.component1()!!)
        }
    }
}

