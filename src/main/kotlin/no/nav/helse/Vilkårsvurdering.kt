package no.nav.helse

import no.nav.helse.sykepenger.vilkar.Vilkårsgrunnlag
import no.nav.helse.sykepenger.vilkar.sykepengevilkår
import no.nav.nare.core.evaluations.Evaluering

fun gjennomførVilkårsvurdering(søknad: AvklartFakta): Evaluering {
    val grunnlag = Vilkårsgrunnlag(
            opptjeningstid = søknad.avklarteVerdier.opptjeningstid.fastsattVerdi.toInt(),
            alder = søknad.avklarteVerdier.alder.fastsattVerdi,
            erMedlem = søknad.avklarteVerdier.medlemsskap.fastsattVerdi,
            ytelser = emptyList(),
            søknadSendt = søknad.originalSøknad.sendtNav!!.toLocalDate(),
            førsteDagSøknadGjelderFor = søknad.originalSøknad.fom,
            fastsattÅrsinntekt = søknad.avklarteVerdier.sykepengegrunnlag.fastsattVerdi.sykepengegrunnlagNårTrygdenYter.fastsattVerdi,
            grunnbeløp = 96883
    )

    return narePrometheus.tellEvaluering { sykepengevilkår.evaluer(grunnlag) }
}
