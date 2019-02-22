package no.nav.helse

import Alder
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.time.LocalDateTime

data class BeriketSykepengesøknad(
        val originalSoknad: Sykepengesoknad,
        val faktagrunnlag: Faktagrunnlag
)

data class AvklartSykepengesoknad(val originalSoknad: Sykepengesoknad,
                                  val medlemskap: Vurdering<Boolean, Tpsfakta>,
                                  val alder: Vurdering<Alder, Tpsfakta>,
                                  val maksdato: Vurdering<LocalDate, Any> = Vurdering.Uavklart(arsak = Vurdering.Uavklart.Arsak.MANGLENDE_DATA, begrunnelse = "Venter på avklart alder og historiske sykepengeperioder", grunnlag = TomtMaksdatoGrunnlag()),
                                  val sykepengeliste: Collection<SykepengerVedtak>,
                                  val arbeidsforhold: Vurdering<Boolean, ArbeidsforholdFakta>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Sykepengesoknad(val aktorId: String,
                           val arbeidsgiver: Arbeidsgiver,
                           val soktUtenlandsopphold: Boolean,
                           val fom: LocalDate,
                           val tom: LocalDate,
                           val startSyketilfelle: LocalDate,
                           val sendtNav: LocalDateTime?,
                           val soknadsperioder: List<Soknadsperiode>,
                           val harVurdertInntekt: Boolean)


data class Arbeidsgiver(val navn : String , val orgnummer : String )

@JsonIgnoreProperties(ignoreUnknown = true)
data class Soknadsperiode(val fom: LocalDate,
                          val tom: LocalDate,
                          val sykmeldingsgrad: Int)

data class Faktagrunnlag(val tps: Tpsfakta,
                         val inntekt: Collection<Inntekt>,
                         val sykepengeliste: Collection<SykepengerVedtak>,
                         val arbeidsforhold: ArbeidsforholdFakta)

data class Tpsfakta(val fodselsdato: LocalDate, val bostedland: String)
