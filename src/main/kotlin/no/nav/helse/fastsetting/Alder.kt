package no.nav.helse.fastsetting

import no.nav.helse.behandling.FaktagrunnlagResultat
import no.nav.helse.behandling.Tpsfakta
import no.nav.nare.core.evaluations.Evaluering
import java.time.LocalDate
import java.time.Period

typealias Alder = Int

data class Aldersgrunnlag(val fodselsdato: LocalDate)

fun vurderAlderPåSisteDagISøknadsPeriode(fakta: FaktagrunnlagResultat): Vurdering<Alder, Aldersgrunnlag> {
    val tpsfakta = fakta.faktagrunnlag.tps
    val tomDato = fakta.originalSøknad.tom
    val alder = tpsfakta.alder(tomDato)
    return Vurdering.Avklart(alder, "§ 8-51", Aldersgrunnlag(fodselsdato = tpsfakta.fodselsdato), "SPA")
}

fun Tpsfakta.alder(dato: LocalDate = LocalDate.now()): Alder = Period.between(fodselsdato, dato).years
