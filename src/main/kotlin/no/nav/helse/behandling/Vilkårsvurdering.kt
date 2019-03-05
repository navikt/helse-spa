package no.nav.helse.behandling

import no.nav.helse.Behandlingsfeil
import no.nav.helse.Either
import no.nav.helse.flatMap
import no.nav.helse.narePrometheus
import no.nav.helse.sykepenger.vilkar.Vilkårsgrunnlag
import no.nav.helse.sykepenger.vilkar.sykepengevilkår
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat

fun vilkårsprøving(avklarteFakta: AvklarteFakta): Either<Behandlingsfeil, Vilkårsprøving> {
    val vilkår = Vilkårsprøving(
            originalSøknad = avklarteFakta.originalSøknad,
            faktagrunnlag = avklarteFakta.faktagrunnlag,
            avklarteVerdier = avklarteFakta.avklarteVerdier,
            vilkårsprøving = gjennomførVilkårsvurdering(avklarteFakta))

    return when(vilkår.vilkårsprøving.resultat) {
        Resultat.JA -> Either.Right(vilkår)
        else -> Either.Left(Behandlingsfeil.vilkårErIkkeOppfylt(vilkår))
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
