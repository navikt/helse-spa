package no.nav.helse.behandling.søknad

import java.time.LocalDate

data class Fravær(
        val fom: LocalDate,
        val tom: LocalDate?,
        val type: Fraværstype
)
