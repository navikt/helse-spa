package no.nav.helse.sensu

import no.nav.helse.assertJsonEquals
import org.json.JSONObject
import org.junit.jupiter.api.Test

class SensuClientTest {

    @Test
    fun `should produce valid json`() {
        val event = SensuEvent("myEvent", "metric", "events_nano", "this is an output")

        val json = JSONObject(event.asJson())

        val expected = JSONObject("""{
    "name": "myEvent",
    "type": "metric",
    "handlers": ["events_nano"],
    "output": "this is an output"
}""".trimMargin())

        assertJsonEquals(expected, json)
    }
}
