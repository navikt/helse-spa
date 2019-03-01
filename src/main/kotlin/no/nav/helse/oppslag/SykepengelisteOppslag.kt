package no.nav.helse.oppslag

import com.github.kittinunf.fuel.httpGet
import no.nav.helse.serde.defaultObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import com.fasterxml.jackson.module.kotlin.*

class SykepengelisteOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    fun hentSykepengeliste(aktorId: String, fom: LocalDate): Collection<SykepengerVedtak> {
        val bearer = stsRestClient.token()
        val (_, _, result) = "$sparkelUrl/api/sykepengevedtak/$aktorId?fom=$fom&tom=${fom.minusYears(3)}".httpGet()
                .header(kotlin.collections.mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to "anything",
                        "Nav-Consumer-Id" to "spa"
                ))
                .responseString()

        return defaultObjectMapper.readValue(result.component1()!!)
    }
}

data class SykepengerVedtak(val fom: LocalDate,
                            val tom: LocalDate,
                            val grad: Float,
                            val mottaker: String,
                            val beløp: BigDecimal)
