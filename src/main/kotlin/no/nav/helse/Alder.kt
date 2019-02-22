package no.nav.helse

import java.time.LocalDate
import java.time.Period

typealias Alder = Int

data class Aldersgrunnlag(val fodselsdato: LocalDate)

fun vurderAlderPåSisteDagISøknadsPeriode(søknad: BeriketSykepengesøknad): Vurdering<Alder, Aldersgrunnlag> {
    val tpsfakta = søknad.faktagrunnlag.tps
    val tomDato = søknad.originalSoknad.tom
    return Vurdering.Avklart(tpsfakta.alder(tomDato), "§ 8-51", Aldersgrunnlag(fodselsdato = tpsfakta.fodselsdato), "SPA")
}

fun Tpsfakta.alder(dato: LocalDate = LocalDate.now()): Alder = Period.between(fodselsdato, dato).years