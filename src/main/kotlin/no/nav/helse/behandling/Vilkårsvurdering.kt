package no.nav.helse.behandling

import no.nav.helse.*
import no.nav.helse.sykepenger.vilkar.Vilkårsgrunnlag
import no.nav.helse.sykepenger.vilkar.sykepengevilkår
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat

fun vilkårsprøving(eitherAvklarteFakta: Either<Behandlingsfeil, AvklarteFakta>): Either<Behandlingsfeil, Vilkårsprøving> = eitherAvklarteFakta.flatMap { avklarteFakta ->
    val vilkårsprøving = Vilkårsprøving(
            originalSøknad = avklarteFakta.originalSøknad,
            faktagrunnlag = avklarteFakta.faktagrunnlag,
            avklarteVerdier = avklarteFakta.avklarteVerdier,
            vilkårsprøving = gjennomførVilkårsvurdering(avklarteFakta))
    when(vilkårsprøving.vilkårsprøving.resultat) {
        Resultat.JA -> Either.Right(vilkårsprøving)
        else -> Either.Left(Behandlingsfeil.from(vilkårsprøving))
    }
}

private fun gjennomførVilkårsvurdering(avklarteFakta: AvklarteFakta): Evaluering {
    val grunnlag = Vilkårsgrunnlag(
            opptjeningstid = avklarteFakta.avklarteVerdier.opptjeningstid.fastsattVerdi.toInt(),
            alder = avklarteFakta.avklarteVerdier.alder.fastsattVerdi,
            erMedlem = avklarteFakta.avklarteVerdier.medlemsskap.fastsattVerdi,
            ytelser = emptyList(),
            søknadSendt = avklarteFakta.originalSøknad.sendtNav!!.toLocalDate(),
            førsteDagSøknadGjelderFor = avklarteFakta.originalSøknad.fom,
            fastsattÅrsinntekt = avklarteFakta.avklarteVerdier.sykepengegrunnlag.fastsattVerdi.sykepengegrunnlagNårTrygdenYter.fastsattVerdi,
            grunnbeløp = 96883
    )

    return narePrometheus.tellEvaluering { sykepengevilkår.evaluer(grunnlag) }
}
