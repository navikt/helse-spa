package no.nav.helse.behandling

import java.time.LocalDate

data class Søknadsperiode(val fom: LocalDate,
                          val tom: LocalDate,
                          val sykmeldingsgrad: Int)
