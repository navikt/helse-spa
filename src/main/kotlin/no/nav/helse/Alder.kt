import no.nav.helse.BeriketSykepengesøknad
import no.nav.helse.Tpsfakta
import no.nav.helse.Vurdering

fun vurderAlderPåSisteDagISøknadsPeriode(søknad: BeriketSykepengesøknad): Vurdering<Int, Tpsfakta> {
    val tpsfakta = søknad.faktagrunnlag.tps
    val tomDato = søknad.originalSoknad.tom
    return Vurdering.Avklart(tpsfakta.alder(tomDato), "§ 8-51", tpsfakta, "SPA")
}