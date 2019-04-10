package no.nav.helse.vilkarsproving

import no.nav.helse.probe.toDatapoints
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.specifications.Spesifikasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NareMetrikkerTest {

    private val leaf1 = Spesifikasjon<String>("a", "1", emptyList()) { Evaluering.ja("Yes") }
    private val leaf2 = Spesifikasjon<String>("b", "2", emptyList()) { Evaluering.ja("Yes") }
    private val leaf3 = Spesifikasjon<String>("c", "3", emptyList()) { Evaluering.ja("Yes") }
    private val leaf4 = Spesifikasjon<String>("d", "4", emptyList()) { Evaluering.ja("Yes") }
    private val node5 = (leaf1 og leaf2).med("5", "ab")
    private val node6 = (leaf3 og leaf4).med("6", "cd")
    private val root = (node5 eller node6).med("7", "abcd")

    @Test
    fun `Konverterer resultatene av en evaluering til InfluxDB vha Sensu`() {
        val datapoints = toDatapoints(root.evaluer("søknad"))
        assertThat(datapoints).hasSize(7)
        assertThat(datapoints.map { it.tags["identifikator"] }).containsExactlyInAnyOrder("1", "2", "3", "4", "5", "6", "7")

    }


}
