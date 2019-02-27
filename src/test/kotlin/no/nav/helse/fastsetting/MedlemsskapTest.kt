package no.nav.helse.fastsetting

import no.nav.helse.BeriketSykepengesøknad
import no.nav.helse.faktagrunnlagUtenVerdi
import no.nav.helse.soknadUtenVerdi
import no.nav.helse.tpsFaktaUtenVerdi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class MedlemsskapTest {

    @Test
    fun `Søker i Norge skal få avklart medlemsskap`() {
        val norskSoknad = soknadForLand("NOR")

        val vurdering = vurderMedlemskap(norskSoknad)

        when (vurdering) {
            is Vurdering.Uavklart -> fail("Norsk søknad skal være avklart")
            is Vurdering.Avklart -> assertThat(vurdering.fastsattVerdi).isTrue()
        }
    }

    @Test
    fun `Søker i Sverige skal ha uavklart medlemsskap`() {
        val norskSoknad = soknadForLand("SVE")

        val vurdering = vurderMedlemskap(norskSoknad)

        when (vurdering) {
            is Vurdering.Avklart -> fail("Søknad med bostedsland Sverige skal være uavklart siden vi ikke vet noe om medlemsskap utover bostedsland foreløpig.")
        }
    }

    @Test
    fun `Søker med bostedsland som ikke følger landskodeverkref skal ha uavklart medlemsskap`() {
        val norskSoknad = soknadForLand("NORGE")

        val vurdering = vurderMedlemskap(norskSoknad)

        when (vurdering) {
            is Vurdering.Avklart -> fail("Søknad med ukjent bostedsland skal være uavklart siden vi ikke vet noe om medlemsskap utover bostedsland foreløpig, og vi ikke skal ta ansvar for å validere kodeverksreferanser.")

        }
    }

    private fun soknadForLand(land: String): BeriketSykepengesøknad = soknadUtenVerdi.copy(faktagrunnlag = faktagrunnlagUtenVerdi.copy(tpsFaktaUtenVerdi.copy(bostedland = land)))
}

