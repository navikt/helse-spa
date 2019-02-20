package no.nav.helse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.kittinunf.fuel.httpPost
import no.nav.helse.serde.defaultObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.LocalDate

class InntektOppslagIT {

    private val LOG = LoggerFactory.getLogger(InntektOppslagIT::class.java.name)

    object Konstanter {
        val mockUrl = "http://localhost:8060"
        val stsRestUrl = "https://vtpmock:8063/stsrest"
        val sparkelUrl = "http://localhost:8080"
    }

    private fun setupSSL() {
        System.setProperty("javax.net.ssl.trustStore","/Users/G153965/mockwork/helse-e2e/mockkeys/truststore.jks")
        System.setProperty("javax.net.ssl.trustStorePassword","changeit")
    }

    @Test
    @Disabled
    fun testInntektOppslag() {
        setupSSL();
        val aktørId = lastMockScenario(50)

        val sts = StsRestClient(
                 Konstanter.stsRestUrl,
                "srvspa",
                "claroquesi")
        val inntektOppslag = InntektOppslag(
                Konstanter.sparkelUrl,
                sts)
        val inntektsfakta = inntektOppslag.hentInntekt(aktørId, LocalDate.now().minusYears(1), LocalDate.now())
        val inntekt = inntektsfakta.registerdata

        LOG.info(inntekt.arbeidsInntektIdentListe.toString())
        Assertions.assertEquals(12, inntekt.arbeidsInntektIdentListe[0].arbeidsInntektMaaned.size)
        val startMåned = 2
        for (i in 0..11) {
            Assertions.assertEquals(
                    if (startMåned + i > 12) startMåned + i - 12 else startMåned + i,
                    inntekt.arbeidsInntektIdentListe[0].arbeidsInntektMaaned[i].aarMaaned.month.value)
            // faktisk ikke tilfelle i testfila per nå:
            /*assertEquals(1,
                    inntektsFakta.arbeidsInntektIdentListe[0].arbeidsInntektMaaned[i].arbeidsInntektInformasjon.inntektListe.size)*/
            Assertions.assertEquals(if (i <= 2) 50000L else 60000L,
                    inntekt.arbeidsInntektIdentListe[0].arbeidsInntektMaaned[i].arbeidsInntektInformasjon.inntektListe[0].beloep)
        }
        println(inntekt);
    }


    private fun lastMockScenario(scenario:Int) : String {
        val (_, _, result) = "${Konstanter.mockUrl}/api/testscenario/$scenario".httpPost()
                .header(mapOf("Accept" to "application/json"))
                .response()
        val scenario = defaultObjectMapper.readValue(result.get(), Scenario::class.java)
        return scenario.personopplysninger.søkerAktørIdent
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Scenario(val personopplysninger:PersonOpplysninger) {
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PersonOpplysninger(val søkerAktørIdent:String) {
    }

}