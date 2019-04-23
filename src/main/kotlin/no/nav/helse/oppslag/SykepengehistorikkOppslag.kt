package no.nav.helse.oppslag

import com.fasterxml.jackson.core.type.TypeReference
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.Either
import no.nav.helse.streams.defaultObjectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class SykepengehistorikkOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    private val log = LoggerFactory.getLogger(SykepengehistorikkOppslag::class.java.name)

    fun hentSykepengehistorikk(aktorId: String, tom: LocalDate): Either<Exception, List<AnvistPeriode>> {
        val bearer = stsRestClient.token()
        val (_, _, result) = "$sparkelUrl/api/sykepengehistorikk/$aktorId?tom=$tom&fom=${tom.minusYears(3)}".httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to UUID.randomUUID().toString(),
                        "Nav-Consumer-Id" to "spa"
                ))
                .responseString()
        val (_, error) = result

        return error?.exception?.let {
            log.error("Error in hentSykepengehistorikk lookup", it)
            Either.Left(it)
        } ?: try {
            Either.Right(defaultObjectMapper.readValue<List<AnvistPeriode>>(result.component1(), object : TypeReference<List<AnvistPeriode>>(){}))
        } catch (err: Exception) {
            Either.Left(err)
        }
    }
}

data class AnvistPeriode(
        val fom: LocalDate,
        val tom: LocalDate
)