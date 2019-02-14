package no.nav.helse

import java.time.LocalDate
import java.time.LocalDateTime

fun hentTPSData(input: Sykepengesoknad): Vurdering<Tpsfakta, Person> {
    val person = hentPerson(AktorId(input.aktorId))

    return Avklart(fastsattVerdi = Tpsfakta(fodselsdato = person.fdato, bostedland = person.bostedsland),
            begrunnelse = "TPS data",
            datoForFastsettelse = LocalDateTime.now(),
            fastsattAv = "SPA",
            grunnlag = person)
}

/*
something clever to fetch person from Sparkel
 */
fun hentPerson(aktorId: AktorId): Person {
    return Person(
            id=AktorId("1234"),
            fornavn = "Joey",
            mellomnavn = "H",
            etternavn = "Sixpack",
            fdato = LocalDate.of(1977, 1, 1),
            kjonn = Kjonn.MANN,
            bostedsland = "Norge"
    )
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