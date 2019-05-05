package no.nav.helse.behandling

import java.time.LocalDate


data class Tpsfakta(val fodselsdato: LocalDate,
                    val bostedland: String?,
                    val statsborgerskap: String,
                    val status: String,
                    val diskresjonskode: String?)
