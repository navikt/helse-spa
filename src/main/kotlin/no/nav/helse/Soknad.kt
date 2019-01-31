package no.nav.helse

import java.time.LocalDate

/**
 * En fullstendig beriket sykepengesøknad
 */
data class Soknad(val id: String, // antagelig en uuid?
                  val aktorId: String?, // AktørId
                  val sykemeldingId: String?, // identity reference
                  val soknadstype: String?,
                  val innsendtDato: LocalDate?,
                  val tom: LocalDate?,
                  val fom: LocalDate?,
                  val opprettetDato: LocalDate?,
                  val status: String,
                  val apneProblemer: Collection<Any> = emptyList()

)