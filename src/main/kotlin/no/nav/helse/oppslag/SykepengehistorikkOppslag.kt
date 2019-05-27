package no.nav.helse.oppslag

import arrow.core.Try
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.streams.defaultObjectMapper
import java.time.LocalDate
import java.util.*

class SykepengehistorikkOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {

    fun hentSykepengehistorikk(aktorId: String, tom: LocalDate): Try<List<AnvistPeriodeDTO>> {
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

        return Try {
            error?.exception?.let {
                throw it
            }

            defaultObjectMapper.readValue<List<AnvistPeriodeDTO>>(result.component1(), object : TypeReference<List<AnvistPeriodeDTO>>(){})
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class AnvistPeriodeDTO(
        val fom: LocalDate,
        val tom: LocalDate
)
