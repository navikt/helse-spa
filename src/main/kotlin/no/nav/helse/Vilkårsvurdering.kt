package no.nav.helse

import no.nav.helse.fastsetting.Vurdering
import no.nav.helse.sykepenger.vilkar.Vilkårsgrunnlag
import no.nav.helse.sykepenger.vilkar.sykepengevilkår
import no.nav.nare.core.evaluations.Evaluering

fun gjennomførVilkårsvurdering(søknad: AvklartSykepengesoknad): Evaluering {
    val grunnlag = Vilkårsgrunnlag(
            førsteSykdomsdag = søknad.originalSøknad.startSyketilfelle,
            datoForAnsettelse = søknad.arbeidsforhold.grunnlag.arbeidsgivere.first().startdato, // this is not right
            alder = (søknad.alder as Vurdering.Avklart).fastsattVerdi,
            erMedlem = (søknad.medlemsskap as Vurdering.Avklart).fastsattVerdi,
            ytelser = emptyList(),
            søknadSendt = søknad.originalSøknad.sendtNav!!.toLocalDate(),
            førsteDagSøknadGjelderFor = søknad.originalSøknad.fom,
            aktuellMånedsinntekt = 10000L,
            rapportertMånedsinntekt = 10000L,
            fastsattÅrsinntekt = (søknad.sykepengegrunnlag as Vurdering.Avklart).fastsattVerdi.sykepengegrunnlagNårTrydenYter.fastsattVerdi,
            grunnbeløp = 96883,
            harVurdertInntekt = false
    )

    return narePrometheus.tellEvaluering { sykepengevilkår.evaluer(grunnlag) }
}