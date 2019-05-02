package no.nav.helse.oppslag.arbeidinntektytelse.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.time.YearMonth

@JsonIgnoreProperties(ignoreUnknown = true)
data class InntektDTO(val virksomhet: VirksomhetDTO,
                      val utbetalingsperiode: YearMonth,
                      val beløp: BigDecimal)
