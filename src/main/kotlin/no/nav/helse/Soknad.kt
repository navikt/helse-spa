package no.nav.helse

import Alder
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

data class BeriketSykepengesøknad(
        val originalSoknad: Sykepengesoknad,
        val faktagrunnlag: Faktagrunnlag
)

data class AvklartSykepengesoknad(val originalSoknad: Sykepengesoknad,
                                  val medlemskap: Vurdering<Boolean, Tpsfakta>,
                                  val alder: Vurdering<Alder, Tpsfakta>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Sykepengesoknad(val aktorId: String,
                           val soktUtenlandsopphold: Boolean,
                           val fom: LocalDate,
                           val tom: LocalDate,
                           val startSyketilfelle: LocalDate,
                           val sendtNav: LocalDateTime?,
                           val soknadsperioder: List<Soknadsperiode>,
                           val harVurdertInntekt: Boolean)

data class Soknadsperiode(val fom: LocalDate,
                          val tom: LocalDate,
                          val sykmeldingsgrad: Int)

data class Faktagrunnlag(val tps: Tpsfakta)

data class Tpsfakta(val fodselsdato: LocalDate, val bostedland: String)
