package no.nav.helse.fastsetting

import assertk.assert
import assertk.assertions.contains
import assertk.assertions.isTrue
import no.nav.helse.tpsFaktaUtenVerdi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class MedlemsskapTest {

    @Test
    fun `Søker i Norge skal få avklart medlemsskap`() {
        val norskSoknad = soknadForLand("NOR", "BOSA")

        val vurdering = vurderMedlemskap(norskSoknad)

        when (vurdering) {
            is Vurdering.Uavklart -> fail("Norsk søknad skal være avklart")
            is Vurdering.Avklart -> {
                assert(vurdering.fastsattVerdi).isTrue()
                assert(vurdering.begrunnelse).contains(søkerOppfyllerKravOmMedlemskap)
            }
        }
    }

    @Test
    fun `Søker i Norge med diskresjonskode skal få uavklart medlemsskap`() {
        val norskSoknad = soknadForLand("NOR", "BOSA", "UFB")

        val vurdering = vurderMedlemskap(norskSoknad)

        when (vurdering) {
            is Vurdering.Avklart -> fail("Norsk søknad skal være uavklart")
            is Vurdering.Uavklart -> {
                assert(vurdering.begrunnelse).contains("Søker har diskresjonskode")
            }
        }
    }

    @Test
    fun `Søker i Sverige skal ha uavklart medlemsskap`() {
        val norskSoknad = soknadForLand("SVE", "BOSA")

        val vurdering = vurderMedlemskap(norskSoknad)

        when (vurdering) {
            is Vurdering.Avklart -> fail("Søknad med bostedsland Sverige skal være uavklart siden vi ikke vet noe om medlemsskap utover bostedsland foreløpig.")
        }
    }

    @Test
    fun `Søker med bostedsland som ikke følger landskodeverkref skal ha uavklart medlemsskap`() {
        val norskSoknad = soknadForLand("NORGE", "BOSA")

        val vurdering = vurderMedlemskap(norskSoknad)

        when (vurdering) {
            is Vurdering.Avklart -> fail("Søknad med ukjent bostedsland skal være uavklart siden vi ikke vet noe om medlemsskap utover bostedsland foreløpig, og vi ikke skal ta ansvar for å validere kodeverksreferanser.")

        }
    }

    private fun soknadForLand(land: String, status: String, diskresjonskode: String? = null) =
            tpsFaktaUtenVerdi.copy(
                    bostedland = land,
                    statsborgerskap = land,
                    status = status,
                    diskresjonskode = diskresjonskode
            )
}

