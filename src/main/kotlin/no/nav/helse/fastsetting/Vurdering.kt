package no.nav.helse.fastsetting

import no.nav.helse.*
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
    class Uavklart<G>(val årsak: Årsak,
                      begrunnelse: String,
                      grunnlag: G): Vurdering<Nothing, G>(begrunnelse, grunnlag) {

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

fun vurderFakta(input: FaktagrunnlagResultat): AvklaringsResultat {
    val medlemsskap = vurderMedlemskap(input)
    val alder = vurderAlderPåSisteDagISøknadsPeriode(input)
    val arbeidsforhold = vurderArbeidsforhold(input)
    val opptjeningstid = vurderOpptjeningstid(Opptjeningsgrunnlag(input.originalSøknad.startSyketilfelle, input.faktagrunnlag.arbeidsforhold.arbeidsgivere))
    val sykepengegrunnlag = fastsettingAvSykepengegrunnlaget(input.originalSøknad.startSyketilfelle, input.originalSøknad.arbeidsgiver, input.faktagrunnlag.beregningsperiode, input.faktagrunnlag.sammenligningsperiode)
    val maksdato = vurderMaksdato(alder,
            input.originalSøknad.startSyketilfelle,
            input.originalSøknad.fom,
            Yrkesstatus.ARBEIDSTAKER,
            input.faktagrunnlag.sykepengeliste)

    return if (listOf(medlemsskap, alder, arbeidsforhold, opptjeningstid, sykepengegrunnlag, maksdato).filter { it is Vurdering.Uavklart<*> }.isNotEmpty()) {
        UavklartFakta(
                originalSøknad = input.originalSøknad,
                faktagrunnlag = input.faktagrunnlag,
                uavklarteVerdier = UavklarteVerdier(
                        medlemsskap = medlemsskap,
                        alder = alder,
                        arbeidsforhold = arbeidsforhold,
                        opptjeningstid = opptjeningstid,
                        sykepengegrunnlag = sykepengegrunnlag,
                        sykepengeliste = input.faktagrunnlag.sykepengeliste,
                        maksdato = maksdato
                )
        )
    } else
        AvklartFakta(
                originalSøknad = input.originalSøknad,
                faktagrunnlag = input.faktagrunnlag,
                avklarteVerdier = AvklarteVerdier(
                        medlemsskap = medlemsskap as Vurdering.Avklart<Boolean, Medlemsskapgrunnlag>,
                        alder = alder as Vurdering.Avklart<Alder, Aldersgrunnlag>,
                        arbeidsforhold = arbeidsforhold as Vurdering.Avklart<Boolean, ArbeidsforholdFakta>,
                        opptjeningstid = opptjeningstid as Vurdering.Avklart<Opptjeningstid, Opptjeningsgrunnlag>,
                        sykepengegrunnlag = sykepengegrunnlag as Vurdering.Avklart<Sykepengegrunnlag, Beregningsperiode>,
                        sykepengeliste = input.faktagrunnlag.sykepengeliste,
                        maksdato = maksdato as Vurdering.Avklart<LocalDate, Any>
                )
        )
}
