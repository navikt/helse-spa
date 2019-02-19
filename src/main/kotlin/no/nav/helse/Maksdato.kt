package no.nav.helse

import java.time.LocalDate

sealed class MaksdatoGrunnlag
data class TomtMaksdatoGrunnlag(val aarsak: String = "Venter på neste steg i fastsetting av fakta") : MaksdatoGrunnlag()
data class FaktiskMaksdatoGrunnlag(val personensAlder: Int, val førsteFraværsdag: LocalDate, val førsteSykepengedag: LocalDate, val yrkesstatus: String, val tidligerePerioder: Collection<Pair<LocalDate, LocalDate>>)

fun vurderMaksdato(soknad: AvklartSykepengesoknad): Vurdering<LocalDate, MaksdatoGrunnlag> = Vurdering.Uavklart(arsak = Vurdering.Uavklart.Arsak.MANGLENDE_DATA, grunnlag = TomtMaksdatoGrunnlag(), begrunnelse = "Ikke implementert")