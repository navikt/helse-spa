package no.nav.helse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.serde.defaultObjectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.temporal.ChronoUnit


class ArbeidsforholdOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    private val log = LoggerFactory.getLogger(ArbeidsforholdOppslag::class.java.name)

    fun hentArbeidsforhold(sykepengesoknad: Sykepengesoknad) : ArbeidsforholdFakta {
        val forsteSykdomsdag = sykepengesoknad.fom
        // Opptjeningstid = minst 4 uker i arbeid før sykdommen
        val fireUkerForSykdomsDag = forsteSykdomsdag.minus(4, ChronoUnit.WEEKS)

        val arbeidsforhold = hentArbeidsforholdRest(AktørId(sykepengesoknad.aktorId), fireUkerForSykdomsDag, forsteSykdomsdag)
        return ArbeidsforholdFakta(arbeidsforhold.organisasjoner.map {
            ArbeidsgiverFakta(it.organisasjonsnummer, it.navn)}, fireUkerForSykdomsDag, forsteSykdomsdag)

    }

    fun hentArbeidsforholdRest(aktørId: AktørId, fom: LocalDate, tom: LocalDate) : Arbeidsforhold {
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

        return defaultObjectMapper.readValue(result.component1(), Arbeidsforhold::class.java)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Arbeidsforhold(val organisasjoner: List<OrganisasjonArbeidsforhold>)
@JsonIgnoreProperties(ignoreUnknown = true)
data class OrganisasjonArbeidsforhold(val organisasjonsnummer: String, val navn: String?)

data class ArbeidsforholdFakta(val arbeidsgivere : List<ArbeidsgiverFakta>, val fom : LocalDate, val tom: LocalDate)
data class ArbeidsgiverFakta(val organisasjonsnummer : String, val navn: String?)
