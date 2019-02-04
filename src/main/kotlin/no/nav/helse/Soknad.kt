package no.nav.helse

import java.time.LocalDate

data class SoknadFraSketch(
        val ansettelsesdato: LocalDate? = null,
        val forste_sykdomsdag: LocalDate? = null,
        val bosted: String? = null,
        val andre_ytelser: String? = null,
        val soknad_sendt: LocalDate? = null
)