package no.nav.helse.oppslag

import com.github.kittinunf.fuel.httpGet
import no.nav.helse.Either
import no.nav.helse.behandling.Sykepengesøknad
import no.nav.helse.behandling.Tpsfakta
import no.nav.helse.map
import no.nav.helse.streams.defaultObjectMapper
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*

class PersonOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {
    private val log = LoggerFactory.getLogger(PersonOppslag::class.java.name)

    fun hentTPSData(input: Sykepengesøknad): Either<Exception, Tpsfakta> {
        return hentPerson(AktørId(input.aktorId)).map { person ->
            Tpsfakta(fodselsdato = person.fdato, bostedland = person.bostedsland)
        }
    }

    private fun hentPerson(aktørId: AktørId): Either<Exception, Person> {
        val bearer = stsRestClient.token()
        val (_, _, result) = "$sparkelUrl/api/person/${aktørId.aktor}".httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to UUID.randomUUID().toString(),
                        "Nav-Consumer-Id" to "spa"
                        ))
                .responseString()

        val (_, error) = result

        return error?.exception?.let {
            log.error("Error in person lookup", it)
            Either.Left(it)
        } ?: try {
            Either.Right(defaultObjectMapper.readValue(result.component1(), PersonDTO::class.java)).map { person ->
                Person(
                        id = AktørId(person.aktørId),
                        fornavn = person.fornavn,
                        mellomnavn = person.mellomnavn,
                        etternavn = person.etternavn,
                        fdato = person.fdato,
                        kjønn = person.kjønn,
                        statsborgerskap = person.statsborgerskap,
                        status = person.status,
                        bostedsland = person.bostedsland,
                        diskresjonskode = person.diskresjonskode

                )
            }
        } catch (err: Exception) {
            Either.Left(err)
        }
    }
}


enum class Kjønn {
    MANN, KVINNE, UKJENN
}

data class PersonDTO(
        val aktørId: String,
        val fornavn: String,
        val mellomnavn: String? = null,
        val etternavn: String,
        val fdato: LocalDate,
        val kjønn: Kjønn,
        val statsborgerskap: String,
        val status: String,
        val bostedsland: String?,
        val diskresjonskode: String?
)

data class Person(
        val id: AktørId,
        val fornavn: String,
        val mellomnavn: String? = null,
        val etternavn: String,
        val fdato: LocalDate,
        val kjønn: Kjønn,
        val statsborgerskap: String,
        val status: String,
        val bostedsland: String? = null,
        val diskresjonskode: String? = null
)

data class AktørId(val aktor: String) {
    init {
        if (aktor.isEmpty()) {
            throw IllegalArgumentException("$aktor cannot be empty")
        }
    }
}

