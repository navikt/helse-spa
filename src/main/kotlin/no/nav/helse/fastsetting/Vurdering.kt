package no.nav.helse.fastsetting

import no.nav.helse.*
import no.nav.helse.behandling.*
import no.nav.helse.domain.*
import java.time.LocalDate
import java.time.LocalDateTime

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
                      begrunnelse: String,
                      grunnlag: G): Vurdering<V, G>(begrunnelse, grunnlag) {

        enum class Årsak {
            KREVER_SKJØNNSMESSIG_VURDERING,
            DÅRLIG_DATAGRUNNLAG,
            MANGELFULL_DATAGRUNNLAG,
            FALLER_UTENFOR_MVP
        }

        override fun toString(): String {
            return "Uavklart(årsak=$årsak, begrunnelse=$begrunnelse)"
        }


    }
}


fun vurderFakta(fakta: FaktagrunnlagResultat): Either<Behandlingsfeil, AvklarteFakta> {
    val medlemsskap = vurderMedlemskap(fakta)
    val alder = vurderAlderPåSisteDagISøknadsPeriode(fakta)
    val arbeidsforhold = vurderArbeidsforhold(fakta)
    val opptjeningstid = vurderOpptjeningstid(Opptjeningsgrunnlag(fakta.originalSøknad.startSyketilfelle, fakta.faktagrunnlag.arbeidsforhold))
    val sykepengegrunnlag = fastsettingAvSykepengegrunnlaget(fakta.originalSøknad.startSyketilfelle, fakta.originalSøknad.arbeidsgiver, fakta.faktagrunnlag.beregningsperiode, fakta.faktagrunnlag.sammenligningsperiode)
    val maksdato = vurderMaksdato(alder,
            fakta.originalSøknad.startSyketilfelle,
            fakta.originalSøknad.fom,
            Yrkesstatus.ARBEIDSTAKER,
            fakta.faktagrunnlag.sykepengeliste)

    return if (listOf(medlemsskap, alder, arbeidsforhold, opptjeningstid, sykepengegrunnlag, maksdato).filter { it is Vurdering.Uavklart<*, *> }.isNotEmpty()) {
        Either.Left(Behandlingsfeil.avklaringsfeil(UavklarteFakta(
                originalSøknad = fakta.originalSøknad,
                faktagrunnlag = fakta.faktagrunnlag,
                uavklarteVerdier = UavklarteVerdier(
                        medlemsskap = medlemsskap,
                        alder = alder,
                        arbeidsforhold = arbeidsforhold,
                        opptjeningstid = opptjeningstid,
                        sykepengegrunnlag = sykepengegrunnlag as Vurdering<Long, Beregningsperiode>,
                        sykepengeliste = fakta.faktagrunnlag.sykepengeliste,
                        maksdato = maksdato
                )
        )))

    } else {
        Either.Right(AvklarteFakta(
                originalSøknad = fakta.originalSøknad,
                faktagrunnlag = fakta.faktagrunnlag,
                avklarteVerdier = AvklarteVerdier(
                        medlemsskap = medlemsskap as Vurdering.Avklart<Boolean, Medlemsskapgrunnlag>,
                        alder = alder as Vurdering.Avklart<Alder, Aldersgrunnlag>,
                        arbeidsforhold = arbeidsforhold as Vurdering.Avklart<Boolean, List<Arbeidsforhold>>,
                        opptjeningstid = opptjeningstid as Vurdering.Avklart<Opptjeningstid, Opptjeningsgrunnlag>,
                        sykepengegrunnlag = sykepengegrunnlag as Vurdering.Avklart<Sykepengegrunnlag, Beregningsperiode>,
                        sykepengeliste = fakta.faktagrunnlag.sykepengeliste,
                        maksdato = maksdato as Vurdering.Avklart<LocalDate, Any>
                )
        ))
    }
}