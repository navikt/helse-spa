package no.nav.helse.vilkarsproving

import assertk.assert
import assertk.assertions.each
import io.mockk.*
import no.nav.helse.sensu.NareReporter
import no.nav.helse.sensu.SensuClient
import no.nav.helse.sensu.SensuEvent
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.specifications.Spesifikasjon
import org.junit.jupiter.api.Test

class NareReporterTest {


    private val leaf1 = Spesifikasjon<String>("a", "1", emptyList()) { Evaluering.ja("Yes") }
    private val leaf2 = Spesifikasjon<String>("b", "2", emptyList()) { Evaluering.ja("Yes") }
    private val leaf3 = Spesifikasjon<String>("c", "3", emptyList()) { Evaluering.ja("Yes") }
    private val leaf4 = Spesifikasjon<String>("d", "4", emptyList()) { Evaluering.ja("Yes") }
    private val node5 = (leaf1 og leaf2).med("5", "ab")
    private val node6 = (leaf3 og leaf4).med("6", "cd")
    private val root = (node5 eller node6).med("7", "abcd")

    @Test
    fun `Konverterer resultatene av en evaluering til InfluxDB vha Sensu`() {

        val sensuClientMock = mockk<SensuClient>()
        val nareReporter = NareReporter(sensuClientMock)

        val capturedEvents = mutableListOf<SensuEvent>()

        every { sensuClientMock.sendEvent(capture(capturedEvents)) } just Runs

        val evaluering = root.evaluer("søknad")

        nareReporter.gjennomførtVilkårsprøving(evaluering)

        verify(exactly = 7) { sensuClientMock.sendEvent(any()) }

        assert(capturedEvents.map { it.output }).each {
           nareReporter.toDatapoints(evaluering).map { "identifikator=" + it.tags["identifikator"].toString() }.contains(it.actual)
        }
    }


}
