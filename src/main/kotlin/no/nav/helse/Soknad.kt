package no.nav.helse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.helse.fastsetting.*
import no.nav.helse.fastsetting.Opptjeningsgrunnlag
import no.nav.helse.fastsetting.Opptjeningstid
import java.time.LocalDate
import java.time.LocalDateTime

interface SoknadDefinition {
    val aktorId: String
    val arbeidsgiver: Arbeidsgiver
    val soktUtenlandsopphold: Boolean
    val fom: LocalDate
    val tom: LocalDate
    val startSyketilfelle: LocalDate
    val sendtNav: LocalDateTime?
    val soknadsperioder: List<Soknadsperiode>
    val harVurdertInntekt: Boolean
}

data class BeriketSykepengesøknad(
        val originalSøknad: SoknadDefinition,
        val faktagrunnlag: Faktagrunnlag
) : SoknadDefinition by originalSøknad

data class AvklartSykepengesoknad(
        val originalSøknad: SoknadDefinition,
        val medlemsskap: Vurdering<Boolean, Medlemsskapgrunnlag>,
        val alder: Vurdering<Alder, Aldersgrunnlag>,
        val maksdato: Vurdering<LocalDate, Any> = Vurdering.Uavklart(årsak = Vurdering.Uavklart.Årsak.MANGELFULL_DATAGRUNNLAG, begrunnelse = "Venter på avklart alder og historiske sykepengeperioder", grunnlag = TomtMaksdatoGrunnlag()),
        val sykepengeliste: Collection<SykepengerVedtak>,
        val arbeidsforhold: Vurdering<Boolean, ArbeidsforholdFakta>,
                                  val opptjeningstid: Vurdering<Opptjeningstid, Opptjeningsgrunnlag>,
        val sykepengegrunnlag: Vurdering<Sykepengegrunnlag, Beregningsperiode>) : SoknadDefinition by originalSøknad

@JsonIgnoreProperties(ignoreUnknown = true)
data class Sykepengesoknad(
        override val aktorId: String,
        override val arbeidsgiver: Arbeidsgiver,
        override val soktUtenlandsopphold: Boolean,
        override val fom: LocalDate,
        override val tom: LocalDate,
        override val startSyketilfelle: LocalDate,
        override val sendtNav: LocalDateTime?,
        override val soknadsperioder: List<Soknadsperiode>,
        override val harVurdertInntekt: Boolean
) : SoknadDefinition


data class Arbeidsgiver(val navn : String , val orgnummer : String )

@JsonIgnoreProperties(ignoreUnknown = true)
data class Soknadsperiode(val fom: LocalDate,
                          val tom: LocalDate,
                          val sykmeldingsgrad: Int)

data class Faktagrunnlag(val tps: Tpsfakta,
                         val beregningsperiode: List<Inntekt>,
                         val sammenligningsperiode: List<Inntekt>,
                         val sykepengeliste: Collection<SykepengerVedtak>,
                         val arbeidsforhold: ArbeidsforholdFakta)

data class Tpsfakta(val fodselsdato: LocalDate, val bostedland: String)
