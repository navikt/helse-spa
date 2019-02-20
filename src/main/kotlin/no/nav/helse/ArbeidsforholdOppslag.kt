package no.nav.helse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.kittinunf.fuel.httpGet
import no.nav.helse.serde.defaultObjectMapper
import java.time.LocalDate

class ArbeidsforholdOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {

    fun hentArbeidsforhold(sykepengesoknad: Sykepengesoknad) : ArbeidsforholdFakta {
        val arbeidsforhold = hentArbeidsforholdRest(AktorId(sykepengesoknad.aktorId), sykepengesoknad.fom, sykepengesoknad.tom)
        return ArbeidsforholdFakta(arbeidsforhold.organisasjoner.map {
            ArbeidsgiverFakta(it.organisasjonsnummer, it.navn)
        })

    }

    fun hentArbeidsforholdRest(aktorId: AktorId, fom : LocalDate, tom: LocalDate) : Arbeidsforhold {
        val bearer = stsRestClient.token()


        val (_, _, result) =
                "$sparkelUrl/api/arbeidsforhold/$aktorId".httpGet(listOf(Pair("fom", fom),Pair("tom", tom)))
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

data class ArbeidsforholdFakta(val arbeidsgiverer : List<ArbeidsgiverFakta>)
data class ArbeidsgiverFakta(val organisasjonsnummer : String, val navn_: String?)