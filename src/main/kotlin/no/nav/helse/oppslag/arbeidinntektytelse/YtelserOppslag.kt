package no.nav.helse.oppslag.arbeidinntektytelse

import arrow.core.Try
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.oppslag.StsRestClient
import no.nav.helse.oppslag.arbeidinntektytelse.dto.YtelserDTO
import no.nav.helse.streams.defaultObjectMapper
import java.time.LocalDate
import java.util.*

class YtelserOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {

    fun hentYtelser(aktorId: String, fom: LocalDate, tom: LocalDate) : Try<YtelserDTO> {
        val bearer = stsRestClient.token()
        val (_, _, result) =
                "$sparkelUrl/api/ytelser/$aktorId?fom=$fom&tom=$tom".httpGet()
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

            defaultObjectMapper.readValue<YtelserDTO>(result.component1()!!)
        }
    }
}
