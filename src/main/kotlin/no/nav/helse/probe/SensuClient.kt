package no.nav.helse.probe

import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.Socket
import kotlin.concurrent.thread

data class SensuEvent(val name: String, val type: String, val handler: String, val output: String, val status: Int = 0) {

    fun asJson() = with(JSONObject()) {
        put("name", name)
        put("type", type)
        put("handlers", JSONArray(listOf(handler)))
        put("output", output)
        put("status", status)
    }.toString()
}

class SensuClient(private val hostname: String, private val port: Int) {

    fun sendEvent(event: SensuEvent) {
        writeToSocket(hostname, port, event.asJson())
        log.debug("Sent event({}) via probe-client", event.asJson())
    }

    companion object {
        private val log = LoggerFactory.getLogger(SensuClient::class.java)

        private fun writeToSocket(hostname: String, port: Int, data: String) {
            thread {
                try {
                    Socket(hostname, port).use { socket ->
                        try {
                            OutputStreamWriter(socket.getOutputStream(), "UTF-8").use { osw ->
                                osw.write(data, 0, data.length)
                                osw.flush()
                                log.debug("wrote {} bytes of data", data.length)
                            }
                        } catch (err: IOException) {
                            log.error("Unable to write data {} to socket", data, err)
                        }
                    }
                } catch (err: ConnectException) {
                    log.error("Unable to connect to {}:{} {}", hostname, port, err.message)
                } catch (err: IOException) {
                    log.error("Unable to connect to {}:{} because of IO problems", hostname, port, err)
                } catch (err: Exception) {
                    log.error("Unable to send event via probe-client", err)
                }
            }
        }
    }
}

