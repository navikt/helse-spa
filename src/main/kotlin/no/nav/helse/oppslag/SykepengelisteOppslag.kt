package no.nav.helse.oppslag

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.Either
import no.nav.helse.streams.defaultObjectMapper
import java.math.BigDecimal
import java.time.LocalDate

class SykepengelisteOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    fun hentSykepengeliste(aktorId: String, tom: LocalDate): Either<Exception, Collection<SykepengerPeriode>> {
        val bearer = stsRestClient.token()
        val (_, _, result) = "$sparkelUrl/api/sykepengeperiode/$aktorId?tom=$tom&fom=${tom.minusYears(3)}".httpGet()
                .header(kotlin.collections.mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to "anything",
                        "Nav-Consumer-Id" to "spa"
                ))
                .responseString()

        return try {
            Either.Right(defaultObjectMapper.readValue(result.get()))
        } catch (e: Exception) {
            Either.Left(e)
        }
    }
}

data class SykepengerPeriode(val fom: LocalDate,
                             val tom: LocalDate,
                             val dagsats: BigDecimal,
                             val grad: Float)
