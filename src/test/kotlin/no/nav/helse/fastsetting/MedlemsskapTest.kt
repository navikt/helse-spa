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
    fun `Søker i Sverige skal få avslått medlemsskap`() {
        val norskSoknad = soknadForLand("SVE")

        val vurdering = vurderMedlemskap(norskSoknad)

        when (vurdering) {
            is Vurdering.Uavklart -> fail("Søknad med bostedsland Sverige skal være avklart som avslått")
            is Vurdering.Avklart -> assertThat(vurdering.fastsattVerdi).isFalse()
        }
    }

    @Test
    fun `Søker med bostedsland som ikke følger landskodeverkref skal få avslått medlemsskap`() {
        val norskSoknad = soknadForLand("NORGE")

        val vurdering = vurderMedlemskap(norskSoknad)

        when (vurdering) {
            is Vurdering.Uavklart -> fail("Søknaden har feil i beriket verdi fra TPS")
            is Vurdering.Avklart -> assertThat(vurdering.fastsattVerdi).isFalse()
        }
    }

    private fun soknadForLand(land: String): BeriketSykepengesøknad = soknadUtenVerdi.copy(faktagrunnlag = faktagrunnlagUtenVerdi.copy(tpsFaktaUtenVerdi.copy(bostedland = land)))
}

