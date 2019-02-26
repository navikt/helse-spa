package no.nav.helse.fastsetting

import no.nav.helse.AvklartSykepengesoknad
import no.nav.helse.Grunnlagsdata
import no.nav.helse.Tidsperiode
import no.nav.helse.Yrkesstatus
import no.nav.helse.maksdato
import java.time.LocalDate

data class TomtMaksdatoGrunnlag(val aarsak: String = "Venter på neste steg i fastsetting av fakta")

fun vurderMaksdato(soknad: AvklartSykepengesoknad): Vurdering<LocalDate, Any> {
    val alder = soknad.alder
    return when (alder) {
        is Vurdering.Uavklart -> Vurdering.Uavklart(årsak = Vurdering.Uavklart.Årsak.MANGELFULL_DATAGRUNNLAG, begrunnelse = "Kan ikke fastsette maksdato for bruker med uavklart alder", grunnlag = TomtMaksdatoGrunnlag(aarsak = "Alder ikke avklart"))
        is Vurdering.Avklart -> {
            val grunnlag = Grunnlagsdata(
                    førsteFraværsdag = soknad.originalSoknad.startSyketilfelle,
                    førsteSykepengedag = soknad.originalSoknad.fom,
                    personensAlder = alder.fastsattVerdi,
                    yrkesstatus = Yrkesstatus.ARBEIDSTAKER, // FIXME: this is not necessarily true
                    tidligerePerioder = soknad.sykepengeliste.map { Tidsperiode(fom = it.fom, tom = it.tom) }
            )
            val beregnetMaksdato = maksdato(grunnlag)

            Vurdering.Avklart(
                    fastsattVerdi = beregnetMaksdato.dato,
                    fastsattAv = "SPA",
                    grunnlag = grunnlag,
                    begrunnelse = beregnetMaksdato.begrunnelse)
        }
    }
}
