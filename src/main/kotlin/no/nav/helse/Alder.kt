import no.nav.helse.BeriketSykepengesøknad
import no.nav.helse.Tpsfakta
import no.nav.helse.Vurdering
import java.time.LocalDate
import java.time.Period

typealias Alder = Int

fun vurderAlderPåSisteDagISøknadsPeriode(søknad: BeriketSykepengesøknad): Vurdering<Alder, Tpsfakta> {
    val tpsfakta = søknad.faktagrunnlag.tps
    val tomDato = søknad.originalSoknad.tom
    return Vurdering.Avklart(tpsfakta.alder(tomDato), "§ 8-51", tpsfakta, "SPA")
}

fun Tpsfakta.alder(dato: LocalDate = LocalDate.now()): Alder = Period.between(fodselsdato, dato).years