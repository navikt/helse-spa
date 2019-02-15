package no.nav.helse

import java.net.URL
import java.time.LocalDate

class PersonOppslag(val sparkelUrl: String) {
    fun hentTPSData(input: Sykepengesoknad): Tpsfakta {
        val person = hentPerson(AktorId(input.aktorId))
        return Tpsfakta(fodselsdato = person.fdato, bostedland = person.bostedsland)
    }

    /*
    something clever to fetch person from Sparkel
     */
    private fun hentPerson(aktorId: AktorId): Person {
        URL("$sparkelUrl/api/person/${aktorId.aktor}").openStream().use { stream ->

        }
        return Person(
                id = AktorId("1234"),
                fornavn = "Joey",
                mellomnavn = "H",
                etternavn = "Sixpack",
                fdato = LocalDate.of(1977, 1, 1),
                kjonn = Kjonn.MANN,
                bostedsland = "Norge"
        )
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

