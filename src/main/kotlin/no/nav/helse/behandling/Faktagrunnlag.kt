package no.nav.helse.behandling

import no.nav.helse.oppslag.AnvistPeriode
import no.nav.helse.oppslag.Inntekt
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidInntektYtelseDTO

data class Faktagrunnlag(val tps: Tpsfakta,
                         val beregningsperiode: List<Inntekt>,
                         val sammenligningsperiode: List<Inntekt>,
                         val sykepengehistorikk: List<AnvistPeriode>,
                         val arbeidInntektYtelse: ArbeidInntektYtelseDTO)
