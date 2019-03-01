package no.nav.helse.oppslag

import com.github.kittinunf.fuel.*
import no.nav.helse.behandling.*
import no.nav.helse.domain.*
import no.nav.helse.serde.*
import org.slf4j.*
import java.time.*
import java.time.temporal.*

class ArbeidsforholdOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    private val log = LoggerFactory.getLogger(ArbeidsforholdOppslag::class.java.name)

    fun hentArbeidsforhold(sykepengesøknad: Sykepengesøknad) : List<Arbeidsforhold> {
        val forsteSykdomsdag = sykepengesøknad.startSyketilfelle
        // Opptjeningstid = minst 4 uker i arbeid før sykdommen
        val fireUkerForSykdomsDag = forsteSykdomsdag.minus(4, ChronoUnit.WEEKS)

        return hentArbeidsforholdRest(AktørId(sykepengesøknad.aktorId), fireUkerForSykdomsDag, forsteSykdomsdag).toList()
    }

    fun hentArbeidsforholdRest(aktørId: AktørId, fom: LocalDate, tom: LocalDate) : Array<Arbeidsforhold> {
        val bearer = stsRestClient.token()
        val (_, _, result) =
                "$sparkelUrl/api/arbeidsforhold/${aktørId.aktor}?fom=$fom&tom=$tom".httpGet()
                        .header(mapOf(
                                "Authorization" to "Bearer $bearer",
                                "Accept" to "application/json",
                                "Nav-Call-Id" to "anything",
                                "Nav-Consumer-Id" to "spa"
                        ))
                        .responseString()

        return defaultObjectMapper.readValue(result.component1(), Array<Arbeidsforhold>::class.java)
    }
}

