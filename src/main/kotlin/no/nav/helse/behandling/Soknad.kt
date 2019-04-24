package no.nav.helse.behandling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.helse.Behandlingsfeil
import no.nav.helse.Either
import no.nav.helse.Grunnlagsdata
import no.nav.helse.domain.Arbeidsforhold
import no.nav.helse.domain.ArbeidsgiverFraSøknad
import no.nav.helse.fastsetting.*
import no.nav.helse.oppslag.AnvistPeriode
import no.nav.helse.oppslag.Inntekt
import no.nav.helse.streams.defaultObjectMapper
import no.nav.helse.sykepenger.beregning.Beregningsresultat
import no.nav.nare.core.evaluations.Evaluering
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class ArbeidsgiverDTO(val navn: String, val orgnummer: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SykepengesøknadV2DTO(
        val id: String,
        val aktorId: String,
        val type: String,
        val status: String,
        val arbeidsgiver: ArbeidsgiverDTO,
        val soktUtenlandsopphold: Boolean,
        val fom: LocalDate,
        val tom: LocalDate,
        val startSyketilfelle: LocalDate,
        val sendtNav: LocalDateTime?,
        val soknadsperioder: List<Soknadsperiode>
)

fun SykepengesøknadV2DTO.mapToSykepengesøknad(): Either<Behandlingsfeil, Sykepengesøknad> {
    return if (sendtNav == null) {
        Either.Left(Behandlingsfeil.ukjentDeserialiseringsfeil(id, defaultObjectMapper.valueToTree(this), Exception("sendtNav er null")))
    } else {
        Either.Right(Sykepengesøknad(
                id = id,
                aktorId = aktorId,
                type = type,
                status = status,
                arbeidsgiver = ArbeidsgiverFraSøknad(arbeidsgiver.navn, arbeidsgiver.orgnummer),
                soktUtenlandsopphold = soktUtenlandsopphold,
                fom = fom,
                tom = tom,
                startSyketilfelle = startSyketilfelle,
                sendtNav = sendtNav,
                soknadsperioder = soknadsperioder
        ))
    }
}

data class Sykepengesøknad(
        val id: String,
        val aktorId: String,
        val type: String,
        val status: String,
        val arbeidsgiver: ArbeidsgiverFraSøknad,
        val soktUtenlandsopphold: Boolean,
        val fom: LocalDate,
        val tom: LocalDate,
        val startSyketilfelle: LocalDate,
        val sendtNav: LocalDateTime,
        val soknadsperioder: List<Soknadsperiode>
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
        val medlemsskap: Vurdering.Avklart<Boolean, Tpsfakta>,
        val alder: Vurdering.Avklart<Alder, Aldersgrunnlag>,
        val maksdato: Vurdering.Avklart<LocalDate, Grunnlagsdata>,
        val sykepengehistorikk: List<AnvistPeriode>,
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
        val medlemsskap: Vurdering<Boolean, Tpsfakta>,
        val alder: Vurdering<Alder, Aldersgrunnlag>,
        val maksdato: Vurdering<LocalDate, Grunnlagsdata?>,
        val sykepengehistorikk: List<AnvistPeriode>,
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

data class Behandlingsgrunnlag(
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

data class Vedtak(val perioder: List<Vedtaksperiode> = emptyList())
data class Vedtaksperiode(
        val fom: LocalDate,
        val tom: LocalDate,
        val dagsats: BigDecimal,
        val grad: Int = 100,
        val fordeling: List<Fordeling>
)
data class Fordeling(
        val mottager: String,
        val andel: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Soknadsperiode(val fom: LocalDate,
                          val tom: LocalDate,
                          val sykmeldingsgrad: Int)

data class Faktagrunnlag(val tps: Tpsfakta,
                         val beregningsperiode: List<Inntekt>,
                         val sammenligningsperiode: List<Inntekt>,
                         val sykepengehistorikk: List<AnvistPeriode>,
                         val arbeidsforhold: List<Arbeidsforhold>)

data class Tpsfakta(val fodselsdato: LocalDate,
                    val bostedland: String?,
                    val statsborgerskap: String,
                    val status: String,
                    val diskresjonskode: String?)

fun asNewPeriode(it: SykepengesøknadV1Periode): Soknadsperiode = Soknadsperiode(
        fom = it.fom,
        tom = it.tom,
        sykmeldingsgrad = it.grad
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SykepengesøknadV1DTO(
        val aktorId: String,
        val status: String,
        val arbeidsgiver: String?,
        val fom: LocalDate?,
        val tom: LocalDate?,
        val startSykeforlop: LocalDate?,
        val innsendtDato: LocalDate?,
        val soknadPerioder: List<SykepengesøknadV1Periode>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SykepengesøknadV1Periode(
        val fom: LocalDate,
        val tom: LocalDate,
        val grad: Int
)
