package no.nav.helse.fastsetting

import no.nav.helse.behandling.FaktagrunnlagResultat
import no.nav.helse.behandling.Tpsfakta
import java.time.LocalDate
import java.time.Period

typealias Alder = Int

data class Aldersgrunnlag(val fodselsdato: LocalDate)

const val begrunnelse_p_8_51 = "§ 8-51"

fun vurderAlderPåSisteDagISøknadsPeriode(fakta: FaktagrunnlagResultat): Vurdering<Alder, Aldersgrunnlag> {
    val tpsfakta = fakta.faktagrunnlag.tps
    val tomDato = fakta.originalSøknad.tom
    val alder = tpsfakta.alder(tomDato)
    return Vurdering.Avklart(alder, begrunnelse_p_8_51, Aldersgrunnlag(fodselsdato = tpsfakta.fodselsdato), "SPA")
}

fun Tpsfakta.alder(dato: LocalDate = LocalDate.now()): Alder = Period.between(fodselsdato, dato).years
