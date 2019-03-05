package no.nav.helse.fastsetting

import no.nav.helse.Grunnlagsdata
import no.nav.helse.Tidsperiode
import no.nav.helse.Yrkesstatus
import no.nav.helse.maksdato
import no.nav.helse.oppslag.SykepengerVedtak
import java.time.LocalDate

data class TomtMaksdatoGrunnlag(val aarsak: String = "Venter på neste steg i fastsetting av fakta")

fun vurderMaksdato(
        alder: Vurdering<Int, Aldersgrunnlag>,
        startSyketilfelle: LocalDate,
        førsteSykepengedag: LocalDate,
        yrkesstatus: Yrkesstatus,
        sykepengeliste: Collection<SykepengerVedtak>
): Vurdering<LocalDate, Any> {
    return when (alder) {
        is Vurdering.Uavklart -> Vurdering.Uavklart(årsak = Vurdering.Uavklart.Årsak.MANGELFULL_DATAGRUNNLAG, begrunnelse = "Kan ikke fastsette maksdato for bruker med uavklart alder", grunnlag = TomtMaksdatoGrunnlag(aarsak = "Alder ikke avklart"))
        is Vurdering.Avklart -> {
            val grunnlag = Grunnlagsdata(
                    førsteFraværsdag = startSyketilfelle,
                    førsteSykepengedag = førsteSykepengedag,
                    personensAlder = alder.fastsattVerdi,
                    yrkesstatus = yrkesstatus,
                    tidligerePerioder = sykepengeliste.map { Tidsperiode(fom = it.fom, tom = it.tom) }
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
