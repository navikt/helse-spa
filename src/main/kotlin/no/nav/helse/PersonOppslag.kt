package no.nav.helse

import com.github.kittinunf.fuel.httpGet
import no.nav.helse.serde.defaultObjectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDate

class PersonOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    private val LOG = LoggerFactory.getLogger(PersonOppslag::class.java.name)

    fun hentTPSData(input: Sykepengesoknad): Tpsfakta {
        val person = hentPerson(AktorId(input.aktorId))
        return Tpsfakta(fodselsdato = person.fdato, bostedland = person.bostedsland)
    }

    private fun hentPerson(aktorId: AktorId): Person {
        val bearer = stsRestClient.token()
        LOG.info("calling hent person with $aktorId")
        val (_, _, result) = "$sparkelUrl/api/person/$aktorId".httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to "anything",
                        "Nav-Consumer-Id" to "spa"
                        ))
                .responseString()

        return defaultObjectMapper.readValue(result.component1(), Person::class.java)
    }
}


enum class Kjonn {
    MANN, KVINNE, UKJENN
}

data class Person(
        val id: AktorId,
        val fornavn: String,
        val mellomnavn: String? = null,
        val etternavn: String,
        val fdato: LocalDate,
        val kjonn: Kjonn,
        val bostedsland: String
)

data class AktorId(val aktor: String) {
    init {
        if (aktor.isEmpty()) {
            throw IllegalArgumentException("$aktor cannot be empty")
        }
    }
}

