package no.nav.helse.fastsetting

import no.nav.helse.Grunnlagsdata
import no.nav.helse.Tidsperiode
import no.nav.helse.Yrkesstatus
import no.nav.helse.maksdato
import no.nav.helse.oppslag.PeriodeYtelse
import java.time.LocalDate

fun vurderMaksdato(
        alder: Vurdering<Int, Aldersgrunnlag>,
        startSyketilfelle: LocalDate,
        førsteSykepengedag: LocalDate,
        yrkesstatus: Yrkesstatus,
        sykepengeliste: List<PeriodeYtelse>
): Vurdering<LocalDate, Grunnlagsdata?> {
    return when (alder) {
        is Vurdering.Uavklart -> Vurdering.Uavklart(årsak = Vurdering.Uavklart.Årsak.HAR_IKKE_DATA, begrunnelse = "Kan ikke fastsette maksdato for bruker med uavklart alder", grunnlag = null)
        is Vurdering.Avklart -> {
            val tidligerePerioder = finnTidligerePerioder(sykepengeliste)
            when (tidligerePerioder) {
                is Vurdering.Uavklart -> Vurdering.Uavklart(årsak = tidligerePerioder.årsak, begrunnelse = tidligerePerioder.begrunnelse, grunnlag = null)
                is Vurdering.Avklart -> {
                    val grunnlag = Grunnlagsdata(
                            førsteFraværsdag = startSyketilfelle,
                            førsteSykepengedag = førsteSykepengedag,
                            personensAlder = alder.fastsattVerdi,
                            yrkesstatus = yrkesstatus,
                            tidligerePerioder = tidligerePerioder.fastsattVerdi
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
    }
}

fun finnTidligerePerioder(sykepengeliste : List<PeriodeYtelse>) : Vurdering<List<Tidsperiode>, List<PeriodeYtelse>> {
    val tidligerePerioder = sykepengeliste.flatMap { periodeYtelse ->
        periodeYtelse.vedtakListe.filter { it.utbetalingsgrad > 0 }
                .map {
                    if (it.anvistPeriode.fom == null || it.anvistPeriode.tom == null) {
                        return Vurdering.Uavklart(Vurdering.Uavklart.Årsak.FORSTÅR_IKKE_DATA, "det finnes en anvist periode hvor fom eller tom er NULL", sykepengeliste)
                    }
                    if (it.anvistPeriode.fom.isAfter(it.anvistPeriode.tom)) {
                        return Vurdering.Uavklart(Vurdering.Uavklart.Årsak.FORSTÅR_IKKE_DATA, "det finnes en anvist periode hvor fom er etter tom", sykepengeliste)
                    }
                    Tidsperiode(fom = it.anvistPeriode.fom, tom = it.anvistPeriode.tom)
                }
    }
    return Vurdering.Avklart(tidligerePerioder, "infotrygdBeregningsgrunnlag", sykepengeliste, "SPA")
}
