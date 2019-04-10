package no.nav.helse.probe

import no.nav.helse.assertJsonEquals
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import kotlin.streams.toList

class SensuClientIntegrationTest {

    companion object {
        private val server = ServerSocket(0)

        @AfterAll
        @JvmStatic
        fun `close server socket`() {
            server.close()
        }
    }

    private fun readFromSocket() =
            with (server) {
                soTimeout = 1000

                accept().use { socket ->
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    reader.lines().toList()
                }
            }

    @Test
    fun `should send event without fields and tags`() {
        val client = SensuClient("localhost", server.localPort)

        val dataPoint = DataPoint("myEvent", mapOf("field1" to "val1", "field2" to 1), mapOf("tag1" to "tag2"))
        val event = InfluxMetricReporter(client, "check-app").sendDataPoint(dataPoint)

        client.sendEvent(event)

        val jsonString = readFromSocket().joinToString(separator = "")

        val json = JSONObject(jsonString)

        val expected = with(JSONObject()) {
            put("name", "check-app")
            put("type", "metric")
            put("handlers", JSONArray(listOf("events_nano")))
            put("output", dataPoint.toLineProtocol())
            put("status", 0)
        }

        assertJsonEquals(expected, json)
    }
}
