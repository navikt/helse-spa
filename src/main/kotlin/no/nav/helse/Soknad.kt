package no.nav.helse

import java.time.LocalDate

data class SoknadFraSketch(
        val ansettelsesDato: LocalDate? = null,
        val forsteSykdomsdag: LocalDate? = null,
        val bosted: String? = null,
        val andreYtelser: String? = null,
        val soknadSendtDato: LocalDate? = null
)