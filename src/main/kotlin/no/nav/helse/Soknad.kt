package no.nav.helse

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.helse.fastsetting.*
import no.nav.helse.fastsetting.Opptjeningsgrunnlag
import no.nav.helse.fastsetting.Opptjeningstid
import no.nav.helse.fastsetting.Vurdering.Avklart
import no.nav.helse.fastsetting.Vurdering.Uavklart
import no.nav.helse.sykepenger.beregning.Beregningsresultat
import no.nav.nare.core.evaluations.Evaluering
import java.time.LocalDate
import java.time.LocalDateTime

interface Søknadsdefinisjon {
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

interface Avklaringer {
    val medlemsskap: Vurdering<Boolean, Medlemsskapgrunnlag>
    val alder: Vurdering<Alder, Aldersgrunnlag>
    val maksdato: Vurdering<LocalDate, Any>
    val sykepengeliste: Collection<SykepengerVedtak>
    val arbeidsforhold: Vurdering<Boolean, ArbeidsforholdFakta>
    val opptjeningstid: Vurdering<Opptjeningstid, Opptjeningsgrunnlag>
    val sykepengegrunnlag: Vurdering<Sykepengegrunnlag, Beregningsperiode>
}

data class BeriketSykepengesøknad(
        @JsonIgnore val originalSøknad: Søknadsdefinisjon,
        val faktagrunnlag: Faktagrunnlag
) : Søknadsdefinisjon by originalSøknad

data class AvklartSykepengesoknad(
        @JsonIgnore val originalSøknad: Søknadsdefinisjon,
        override val medlemsskap: Vurdering<Boolean, Medlemsskapgrunnlag>,
        override val alder: Vurdering<Alder, Aldersgrunnlag>,
        override val maksdato: Vurdering<LocalDate, Any> = Uavklart(årsak = Uavklart.Årsak.MANGELFULL_DATAGRUNNLAG, begrunnelse = "Venter på avklart alder og historiske sykepengeperioder", grunnlag = TomtMaksdatoGrunnlag()),
        override val sykepengeliste: Collection<SykepengerVedtak>,
        override val arbeidsforhold: Vurdering<Boolean, ArbeidsforholdFakta>,
        override val opptjeningstid: Vurdering<Opptjeningstid, Opptjeningsgrunnlag>,
        override val sykepengegrunnlag: Vurdering<Sykepengegrunnlag, Beregningsperiode>) : Søknadsdefinisjon by originalSøknad, Avklaringer {

    fun erAvklart(): Boolean {
        return medlemsskap is Avklart
                && alder is Avklart
                && maksdato is Avklart
                && arbeidsforhold is Avklart
                && sykepengegrunnlag is Avklart
    }
}

data class BeregnetSykepengesoknad(val vilkårsprøvdSøknad : AvklartSykepengesoknad/*TODO: SKal være Vilkårsprøvd søknad*/,
                                   val beregning : Beregningsresultat)

data class VilkårsprøvdSykepengesøknad(
        @JsonIgnore val originalSøknad: AvklartSykepengesoknad,
        val vilkårsvurdering: Evaluering
) : Søknadsdefinisjon by originalSøknad, Avklaringer by originalSøknad

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
) : Søknadsdefinisjon


data class Arbeidsgiver(val navn: String, val orgnummer: String)

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
