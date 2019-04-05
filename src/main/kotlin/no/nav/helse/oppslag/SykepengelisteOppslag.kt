package no.nav.helse.oppslag

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.Either
import no.nav.helse.streams.defaultObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class SykepengelisteOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    fun hentSykepengeliste(aktorId: String, tom: LocalDate): Either<Exception, Collection<SykepengerPeriode>> {
        if ("dev-fss" == System.getenv("NAIS_CLUSTER_NAME")) {
            val bearer = stsRestClient.token()
            val (_, _, result) = "$sparkelUrl/api/sykepengeperiode/$aktorId?tom=$tom&fom=${tom.minusYears(3)}".httpGet()
                    .header(kotlin.collections.mapOf(
                            "Authorization" to "Bearer $bearer",
                            "Accept" to "application/json",
                            "Nav-Call-Id" to UUID.randomUUID().toString(),
                            "Nav-Consumer-Id" to "spa"
                    ))
                    .responseString()

            return try {
                Either.Right(defaultObjectMapper.readValue(result.get()))
            } catch (e: Exception) {
                Either.Left(e)
            }
        } else {
            return Either.Right(emptyList())
        }
    }

}

data class SykepengerPeriode(val fom: LocalDate,
                             val tom: LocalDate,
                             val dagsats: BigDecimal,
                             val grad: Float)
