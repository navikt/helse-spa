package no.nav.helse

import no.nav.helse.fastsetting.Vurdering
import no.nav.helse.sykepenger.vilkar.Vilkårsgrunnlag
import no.nav.helse.sykepenger.vilkar.sykepengevilkår
import no.nav.nare.core.evaluations.Evaluering

fun gjennomførVilkårsvurdering(søknad: AvklartSykepengesoknad): Evaluering {
    val grunnlag = Vilkårsgrunnlag(
            opptjeningstid = (søknad.opptjeningstid as Vurdering.Avklart).fastsattVerdi.toInt(),
            alder = (søknad.alder as Vurdering.Avklart).fastsattVerdi,
            erMedlem = (søknad.medlemsskap as Vurdering.Avklart).fastsattVerdi,
            ytelser = emptyList(),
            søknadSendt = søknad.originalSøknad.sendtNav!!.toLocalDate(),
            førsteDagSøknadGjelderFor = søknad.originalSøknad.fom,
            fastsattÅrsinntekt = (søknad.sykepengegrunnlag as Vurdering.Avklart).fastsattVerdi.sykepengegrunnlagNårTrydenYter.fastsattVerdi,
            grunnbeløp = 96883
    )

    return narePrometheus.tellEvaluering { sykepengevilkår.evaluer(grunnlag) }
}
