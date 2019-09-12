package no.nav.helse.fastsetting

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.behandling.FaktagrunnlagResultat
import no.nav.helse.behandling.søknad.Sykepengesøknad
import no.nav.helse.behandling.søknad.tilSakskompleks
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

    private fun soknadForDato(tom: LocalDate, foedselsDato: LocalDate): FaktagrunnlagResultat = soknadUtenVerdi.copy(
            sakskompleks = Sykepengesøknad(
                    jsonNode = with(originalSoknad.jsonNode) {
                        val objectNode: ObjectNode = deepCopy()
                        objectNode.put("tom", tom.toString())
                        objectNode
                    }
            ).tilSakskompleks(),
            faktagrunnlag = faktagrunnlagUtenVerdi.copy(
                    tpsFaktaUtenVerdi.copy(
                            fodselsdato = foedselsDato)
            )
    )
}

