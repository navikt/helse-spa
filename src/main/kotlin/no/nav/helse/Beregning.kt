package no.nav.helse

import no.nav.helse.fastsetting.Vurdering
import no.nav.helse.sykepenger.beregning.Beregningsgrunnlag

fun grunnbeløp() = 96883L // TODO: lookup?

fun lagBeregninggrunnlag(soknad: AvklartSykepengesoknad) : Beregningsgrunnlag =
        Beregningsgrunnlag(
                fom = soknad.originalSøknad.fom, // er dette første dag etter arbeidsgiverperiode ?
                ferie = null,
                permisjon = null,
                sykmeldingsgrad = soknad.originalSøknad.soknadsperioder.let {
                    if (it.size == 1) it[0].sykmeldingsgrad else throw Exception("takler bare én periode per nå")
                },
                sykepengegrunnlag =  no.nav.helse.sykepenger.beregning.Sykepengegrunnlag(
                        fastsattInntekt = (soknad.sykepengegrunnlag as Vurdering.Avklart).fastsattVerdi.sykepengegrunnlagNårTrydenYter.fastsattVerdi,
                        grunnbeløp = grunnbeløp()),
                sisteUtbetalingsdato = ((soknad.maksdato as Vurdering.Avklart).fastsattVerdi).let {
                    if (it.isBefore(soknad.originalSøknad.tom)) it else soknad.originalSøknad.tom
                })

