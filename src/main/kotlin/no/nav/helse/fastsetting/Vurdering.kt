package no.nav.helse.fastsetting

import arrow.core.Either
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.prometheus.client.Counter
import no.nav.helse.Behandlingsfeil
import no.nav.helse.Grunnlagsdata
import no.nav.helse.Yrkesstatus
import no.nav.helse.behandling.*
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsforholdDTO
import java.time.LocalDate
import java.time.LocalDateTime

private val vurderingerCounter = Counter.build()
        .name("vurderinger_totals")
        .labelNames("hva", "resultat")
        .help("antall vurderinger gjort, fordelt på uavklart eller avklart")
        .register()

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = Vurdering.Uavklart::class, name = "Uavklart"),
        JsonSubTypes.Type(value = Vurdering.Avklart::class, name = "Avklart"))
sealed class Vurdering<out V, out G>(val begrunnelse: String, val grunnlag: G, val vurderingstidspunkt: LocalDateTime = LocalDateTime.now()) {
    class Avklart<V, G>(val fastsattVerdi: V,
                        begrunnelse: String,
                        grunnlag: G,
                        val fastsattAv: String): Vurdering<V, G>(begrunnelse, grunnlag) {

        override fun toString(): String {
            return "Avklart(fastsattVerdi=$fastsattVerdi, begrunnelse=$begrunnelse)"
        }

    }
    class Uavklart<V,G>(val årsak: Årsak,
                        val underårsak: String = "UKJENT",
                        begrunnelse: String,
                        grunnlag: G): Vurdering<V, G>(begrunnelse, grunnlag) {

        enum class Årsak {
            KREVER_SKJØNNSMESSIG_VURDERING,
            FORSTÅR_IKKE_DATA,
            HAR_IKKE_DATA,
            FALLER_UTENFOR_MVP
        }

        override fun toString(): String {
            return "Uavklart(årsak=$årsak, begrunnelse=$begrunnelse)"
        }


    }
}


fun vurderFakta(fakta: FaktagrunnlagResultat): Either<Behandlingsfeil, AvklarteFakta> {
    val medlemsskap = vurderMedlemskap(fakta.faktagrunnlag.tps).also {
        if (it is Vurdering.Uavklart) {
            vurderingerCounter.labels("medlemsskap", "uavklart").inc()
        } else {
            vurderingerCounter.labels("medlemsskap", "avklart").inc()
        }
    }
    val alder = vurderAlderPåSisteDagISøknadsPeriode(fakta).also {
        if (it is Vurdering.Uavklart) {
            vurderingerCounter.labels("alder", "uavklart").inc()
        } else {
            vurderingerCounter.labels("alder", "avklart").inc()
        }
    }
    val arbeidsforhold = vurderArbeidsforhold(fakta).also {
        if (it is Vurdering.Uavklart) {
            vurderingerCounter.labels("arbeidsforhold", "uavklart").inc()
        } else {
            vurderingerCounter.labels("arbeidsforhold", "avklart").inc()
        }
    }
    val opptjeningstid = vurderOpptjeningstid(Opptjeningsgrunnlag(fakta.originalSøknad.startSyketilfelle, fakta.faktagrunnlag.arbeidInntektYtelse.arbeidsforhold)).also {
        if (it is Vurdering.Uavklart) {
            vurderingerCounter.labels("opptjeningstid", "uavklart").inc()
        } else {
            vurderingerCounter.labels("opptjeningstid", "avklart").inc()
        }
    }
    val sykepengegrunnlag = fastsettingAvSykepengegrunnlaget(fakta.originalSøknad.startSyketilfelle, fakta.originalSøknad.soknadsperioder, fakta.originalSøknad.arbeidsgiver, fakta.faktagrunnlag.beregningsperiode, fakta.faktagrunnlag.sammenligningsperiode).also {
        if (it is Vurdering.Uavklart) {
            vurderingerCounter.labels("sykepengegrunnlag", "uavklart").inc()
        } else {
            vurderingerCounter.labels("sykepengegrunnlag", "avklart").inc()
        }
    }
    val maksdato = vurderMaksdato(alder,
            fakta.originalSøknad.startSyketilfelle,
            fakta.originalSøknad.fom,
            Yrkesstatus.ARBEIDSTAKER,
            fakta.faktagrunnlag.sykepengehistorikk).also {
        if (it is Vurdering.Uavklart) {
            vurderingerCounter.labels("maksdato", "uavklart").inc()
        } else {
            vurderingerCounter.labels("maksdato", "avklart").inc()
        }
    }

    return if (listOf(medlemsskap, alder, arbeidsforhold, opptjeningstid, sykepengegrunnlag, maksdato).filter { it is Vurdering.Uavklart<*, *> }.isNotEmpty()) {
        Either.Left(Behandlingsfeil.avklaringsfeil(UavklarteFakta(
                originalSøknad = fakta.originalSøknad,
                faktagrunnlag = fakta.faktagrunnlag,
                uavklarteVerdier = UavklarteVerdier(
                        medlemsskap = medlemsskap,
                        alder = alder,
                        arbeidsforhold = arbeidsforhold,
                        opptjeningstid = opptjeningstid,
                        sykepengegrunnlag = sykepengegrunnlag,
                        sykepengehistorikk = fakta.faktagrunnlag.sykepengehistorikk,
                        maksdato = maksdato
                )
        )))

    } else {
        @Suppress("UNCHECKED_CAST")
        Either.Right(AvklarteFakta(
                originalSøknad = fakta.originalSøknad,
                faktagrunnlag = fakta.faktagrunnlag,
                avklarteVerdier = AvklarteVerdier(
                        medlemsskap = medlemsskap as Vurdering.Avklart<Boolean, Tpsfakta>,
                        alder = alder as Vurdering.Avklart<Alder, Aldersgrunnlag>,
                        arbeidsforhold = arbeidsforhold as Vurdering.Avklart<Boolean, List<ArbeidsforholdDTO>>,
                        opptjeningstid = opptjeningstid as Vurdering.Avklart<Opptjeningstid, Opptjeningsgrunnlag>,
                        sykepengegrunnlag = sykepengegrunnlag as Vurdering.Avklart<Sykepengegrunnlag, Beregningsperiode>,
                        sykepengehistorikk = fakta.faktagrunnlag.sykepengehistorikk,
                        maksdato = maksdato as Vurdering.Avklart<LocalDate, Grunnlagsdata>
                )
        ))
    }
}
