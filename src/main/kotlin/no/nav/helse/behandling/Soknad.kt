package no.nav.helse.behandling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.helse.domain.Arbeidsforhold
import no.nav.helse.domain.Arbeidsgiver
import no.nav.helse.fastsetting.*
import no.nav.helse.oppslag.Inntekt
import no.nav.helse.oppslag.SykepengerVedtak
import no.nav.helse.sykepenger.beregning.Beregningsresultat
import no.nav.nare.core.evaluations.Evaluering
import java.time.LocalDate
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class Sykepengesøknad(
        val aktorId: String,
        val status: String,
        val arbeidsgiver: Arbeidsgiver,
        val soktUtenlandsopphold: Boolean,
        val fom: LocalDate,
        val tom: LocalDate,
        val startSyketilfelle: LocalDate,
        val sendtNav: LocalDateTime?,
        val soknadsperioder: List<Soknadsperiode>,
        val harVurdertInntekt: Boolean
)

data class FaktagrunnlagResultat(
        val originalSøknad: Sykepengesøknad,
        val faktagrunnlag: Faktagrunnlag
)

sealed class AvklaringsResultat

data class AvklarteFakta(
        val originalSøknad: Sykepengesøknad,
        val faktagrunnlag: Faktagrunnlag,
        val avklarteVerdier: AvklarteVerdier
) : AvklaringsResultat()

data class AvklarteVerdier(
        val medlemsskap: Vurdering.Avklart<Boolean, Medlemsskapgrunnlag>,
        val alder: Vurdering.Avklart<Alder, Aldersgrunnlag>,
        val maksdato: Vurdering.Avklart<LocalDate, Any>,
        val sykepengeliste: Collection<SykepengerVedtak>,
        val arbeidsforhold: Vurdering.Avklart<Boolean, List<Arbeidsforhold>>,
        val opptjeningstid: Vurdering.Avklart<Opptjeningstid, Opptjeningsgrunnlag>,
        val sykepengegrunnlag: Vurdering.Avklart<Sykepengegrunnlag, Beregningsperiode>
)

data class UavklarteFakta(
        val originalSøknad: Sykepengesøknad,
        val faktagrunnlag: Faktagrunnlag,
        val uavklarteVerdier: UavklarteVerdier
) : AvklaringsResultat()

data class UavklarteVerdier(
        val medlemsskap: Vurdering<Boolean, Medlemsskapgrunnlag>,
        val alder: Vurdering<Alder, Aldersgrunnlag>,
        val maksdato: Vurdering<LocalDate, Any>,
        val sykepengeliste: Collection<SykepengerVedtak>,
        val arbeidsforhold: Vurdering<Boolean, List<Arbeidsforhold>>,
        val opptjeningstid: Vurdering<Opptjeningstid, Opptjeningsgrunnlag>,
        val sykepengegrunnlag: Vurdering<*, *>
) {
    fun asNamedList(): List<Pair<String, Vurdering<*, *>>> = listOf(
            Pair("medlemsskap", medlemsskap),
            Pair("alder", alder),
            Pair("maksdato", maksdato),
            Pair("arbeidsforhold", arbeidsforhold),
            Pair("opptjeningstid", opptjeningstid),
            Pair("sykepengegrunnlag", sykepengegrunnlag)
    )
}

data class Vilkårsprøving(
        val originalSøknad: Sykepengesøknad,
        val faktagrunnlag: Faktagrunnlag,
        val avklarteVerdier: AvklarteVerdier,
        val vilkårsprøving: Evaluering
)

data class Sykepengeberegning(
        val originalSøknad: Sykepengesøknad,
        val faktagrunnlag: Faktagrunnlag,
        val avklarteVerdier: AvklarteVerdier,
        val vilkårsprøving: Evaluering,
        val beregning: Beregningsresultat)

data class SykepengeVedtak(
        val originalSøknad: Sykepengesøknad,
        val faktagrunnlag: Faktagrunnlag,
        val avklarteVerdier: AvklarteVerdier,
        val vilkårsprøving: Evaluering,
        val beregning: Beregningsresultat,
        val vedtak: Vedtak
)

data class Vedtak(val resultat: String = "Jeg har ikke laget noe vedtak")

@JsonIgnoreProperties(ignoreUnknown = true)
data class Soknadsperiode(val fom: LocalDate,
                          val tom: LocalDate,
                          val sykmeldingsgrad: Int)

data class Faktagrunnlag(val tps: Tpsfakta,
                         val beregningsperiode: List<Inntekt>,
                         val sammenligningsperiode: List<Inntekt>,
                         val sykepengeliste: Collection<SykepengerVedtak>,
                         val arbeidsforhold: List<Arbeidsforhold>)

data class Tpsfakta(val fodselsdato: LocalDate, val bostedland: String)

fun asNewPeriode(it: LegacySoknadsperiode): Soknadsperiode = Soknadsperiode(
        fom = it.fom,
        tom = it.tom,
        sykmeldingsgrad = it.grad
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LegacySøknad(
        val aktorId: String,
        val status: String,
        val arbeidsgiver: String?,
        val fom: LocalDate?,
        val tom: LocalDate?,
        val startSykeforlop: LocalDate?,
        val innsendtDato: LocalDate?,
        val soknadPerioder: List<LegacySoknadsperiode>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LegacySoknadsperiode(
        val fom: LocalDate,
        val tom: LocalDate,
        val grad: Int
)
