package no.nav.helse.oppslag.arbeidinntektytelse.dto

import java.math.BigDecimal
import java.time.YearMonth

data class YtelseDTO(val virksomhet: VirksomhetDTO, val utbetalingsperiode: YearMonth, val beløp: BigDecimal, val kode: String)
