package no.nav.helse

import org.slf4j.LoggerFactory
import kotlin.concurrent.thread

private val log = LoggerFactory.getLogger("Spa")

fun main() {
    val spa = SaksbehandlingStream(Environment())
    log.info("Opening up the Spa")
    spa.start()
    Runtime.getRuntime().addShutdownHook(thread {
        log.info("Closing the Spa")
        spa.stop()
    })
}

