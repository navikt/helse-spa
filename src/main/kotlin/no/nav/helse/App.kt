package no.nav.helse

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import org.slf4j.LoggerFactory
import java.util.Collections.emptySet

private val log = LoggerFactory.getLogger("App")
private val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry

fun main() {
    DefaultExports.initialize()

    embeddedServer(Netty, 8080) {
        routing {
            get("/isalive") {
                log.trace("/isalive called")
                call.respondText("ALIVE", ContentType.Text.Plain)
            }

            get("/isready") {
                log.trace("/isready called")
                call.respondText("READY", ContentType.Text.Plain)
            }

            get("/metrics") {
                log.trace("/metrics called")

                val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: emptySet()
                call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
                    TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
                }
            }
        }
    }.start(wait = false)
}

