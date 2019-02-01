package no.nav.helse

import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory
import java.time.LocalDate

private val log = LoggerFactory.getLogger("Spa")

fun main() {
    //val spa = SaksbehandlingStream(Environment())
    //log.info("Opening up the Spa")
    //spa.start()

    embeddedServer(Netty, 80) {
        routing {
            routeResources()
            post("/soknad") {

            }
        }
    }.start(wait = false)
}

fun Route.routeResources() {
    static("") {
        resources("css")
        resources("js")
        resources("html")
    }
}