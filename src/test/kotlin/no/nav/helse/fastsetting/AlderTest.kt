package no.nav.helse.fastsetting

import no.nav.helse.BeriketSykepengesøknad
import no.nav.helse.faktagrunnlagUtenVerdi
import no.nav.helse.originalSoknad
import no.nav.helse.soknadUtenVerdi
import no.nav.helse.tpsFaktaUtenVerdi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDate.of

class AlderTest {

    @Test
    fun `Søker under 62 skal få avklart alder`() {
        val soknadUnder62 = soknadForDato(of(1961, 1, 1), of(1900, 1, 1))

        val vurdering = vurderAlderPåSisteDagISøknadsPeriode(soknadUnder62)

        when (vurdering) {
            is Vurdering.Uavklart -> fail("Alder skal være 61")
            is Vurdering.Avklart -> assertThat(vurdering.fastsattVerdi).isEqualTo(61)
        }
    }

    private fun soknadForDato(tom: LocalDate, foedselsDato: LocalDate): BeriketSykepengesøknad = soknadUtenVerdi.copy(
            originalSøknad = originalSoknad.copy(
                    tom = tom
            ),
            faktagrunnlag = faktagrunnlagUtenVerdi.copy(
                    tpsFaktaUtenVerdi.copy(
                            fodselsdato = foedselsDato)
            )
    )
}

