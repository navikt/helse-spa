package no.nav.helse

import no.nav.helse.sykepenger.beregning.Beregningsgrunnlag

fun grunnbeløp() = 96883L // TODO: lookup?

fun lagBeregninggrunnlag(vilkårsprøving: Vilkårsprøving) : Beregningsgrunnlag =
        Beregningsgrunnlag(
                fom = vilkårsprøving.originalSøknad.fom, // er dette første dag etter arbeidsgiverperiode ?
                ferie = null,
                permisjon = null,
                sykmeldingsgrad = vilkårsprøving.originalSøknad.soknadsperioder.let {
                    if (it.size == 1) it[0].sykmeldingsgrad else throw Exception("takler bare én periode per nå")
                },
                sykepengegrunnlag =  no.nav.helse.sykepenger.beregning.Sykepengegrunnlag(
                        fastsattInntekt = vilkårsprøving.avklarteVerdier.sykepengegrunnlag.fastsattVerdi.sykepengegrunnlagNårTrygdenYter.fastsattVerdi,
                        grunnbeløp = grunnbeløp()),
                sisteUtbetalingsdato = (vilkårsprøving.avklarteVerdier.maksdato.fastsattVerdi).let {
                    if (it.isBefore(vilkårsprøving.originalSøknad.tom)) it else vilkårsprøving.originalSøknad.tom
                })

