package no.nav.helse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year

data class BeriketSykepengesoknad(
        val originalSoknad: Sykepengesoknad,
        val faktagrunnlag: Faktagrunnlag
)

data class AvklartSykepengesoknad(val originalSoknad: Sykepengesoknad,
             val medlemskap: Vurdering<Boolean, Tpsfakta>)

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

data class Tpsfakta(val fodselsdato: LocalDate, val bostedland: String) {
    fun alder(): Int {
        val age = Year.now().value - fodselsdato.year
        return if (LocalDate.now().withYear(fodselsdato.year) >= fodselsdato) age else age - 1
    }
}
