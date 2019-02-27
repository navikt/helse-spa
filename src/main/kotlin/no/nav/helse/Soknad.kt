package no.nav.helse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.helse.fastsetting.Alder
import no.nav.helse.fastsetting.Aldersgrunnlag
import no.nav.helse.fastsetting.Beregningsperiode
import no.nav.helse.fastsetting.Medlemsskapgrunnlag
import no.nav.helse.fastsetting.Opptjeningsgrunnlag
import no.nav.helse.fastsetting.Opptjeningstid
import no.nav.helse.fastsetting.Sykepengegrunnlag
import no.nav.helse.fastsetting.TomtMaksdatoGrunnlag
import no.nav.helse.fastsetting.Vurdering
import java.time.LocalDate
import java.time.LocalDateTime

data class BeriketSykepengesøknad(
        val originalSoknad: Sykepengesoknad,
        val faktagrunnlag: Faktagrunnlag
)

data class AvklartSykepengesoknad(val originalSoknad: Sykepengesoknad,
                                  val medlemskap: Vurdering<Boolean, Medlemsskapgrunnlag>,
                                  val alder: Vurdering<Alder, Aldersgrunnlag>,
                                  val maksdato: Vurdering<LocalDate, Any> = Vurdering.Uavklart(årsak = Vurdering.Uavklart.Årsak.MANGELFULL_DATAGRUNNLAG, begrunnelse = "Venter på avklart alder og historiske sykepengeperioder", grunnlag = TomtMaksdatoGrunnlag()),
                                  val sykepengeliste: Collection<SykepengerVedtak>,
                                  val arbeidsforhold: Vurdering<Boolean, ArbeidsforholdFakta>,
                                  val opptjeningstid: Vurdering<Opptjeningstid, Opptjeningsgrunnlag>,
                                  val sykepengegrunnlag: Vurdering<Sykepengegrunnlag, Beregningsperiode>)

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
                         val beregningsperiode: List<Inntekt>,
                         val sammenligningsperiode: List<Inntekt>,
                         val sykepengeliste: Collection<SykepengerVedtak>,
                         val arbeidsforhold: ArbeidsforholdFakta)

data class Tpsfakta(val fodselsdato: LocalDate, val bostedland: String)
