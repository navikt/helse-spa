package no.nav.helse.fastsetting

import no.nav.helse.*
import no.nav.helse.Tidsperiode
import no.nav.helse.oppslag.AnvistPeriodeDTO
import java.time.LocalDate

fun vurderMaksdato(
        alder: Vurdering<Int, Aldersgrunnlag>,
        startSyketilfelle: LocalDate,
        førsteSykepengedag: LocalDate,
        sisteSykepengedag: LocalDate,
        yrkesstatus: Yrkesstatus,
        sykepengehistorikk: List<AnvistPeriodeDTO>
): Vurdering<LocalDate, Grunnlagsdata?> {
    return when (alder) {
        is Vurdering.Uavklart -> Vurdering.Uavklart(årsak = Vurdering.Uavklart.Årsak.HAR_IKKE_DATA, begrunnelse = "Kan ikke fastsette maksdato for bruker med uavklart alder", grunnlag = null)
        is Vurdering.Avklart -> {
            val grunnlag = Grunnlagsdata(
                    førsteFraværsdag = startSyketilfelle,
                    førsteSykepengedag = førsteSykepengedag,
                    personensAlder = alder.fastsattVerdi,
                    yrkesstatus = yrkesstatus,
                    tidligerePerioder = sykepengehistorikk.map { Tidsperiode(it.fom, it.tom) }.sortedByDescending { it.tom }
            )
            val beregnetMaksdato = maksdato(grunnlag)

            if (beregnetMaksdato.dato < sisteSykepengedag)
                Vurdering.Uavklart(
                        årsak = Vurdering.Uavklart.Årsak.FALLER_UTENFOR_MVP, begrunnelse = "Maksdato er tidligere enn siste sykdomsdag, " + beregnetMaksdato.begrunnelse, grunnlag = grunnlag)
            else
                Vurdering.Avklart(
                        fastsattVerdi = beregnetMaksdato.dato,
                        fastsattAv = "SPA",
                        grunnlag = grunnlag,
                        begrunnelse = beregnetMaksdato.begrunnelse)
        }
    }
}
