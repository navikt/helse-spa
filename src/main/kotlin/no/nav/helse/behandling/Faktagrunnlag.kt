package no.nav.helse.behandling

import no.nav.helse.oppslag.AnvistPeriodeDTO
import no.nav.helse.oppslag.Inntekt
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidInntektYtelseDTO
import no.nav.helse.oppslag.arbeidinntektytelse.dto.YtelserDTO

data class Faktagrunnlag(val tps: Tpsfakta,
                         val beregningsperiode: List<Inntekt>,
                         val sammenligningsperiode: List<Inntekt>,
                         val sykepengehistorikk: List<AnvistPeriodeDTO>,
                         val arbeidInntektYtelse: ArbeidInntektYtelseDTO,
                         val ytelser: YtelserDTO)
