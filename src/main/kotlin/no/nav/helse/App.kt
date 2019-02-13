package no.nav.helse

import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("Spa")

fun main() {
    val spa = SaksbehandlingStream(Environment())
    log.info("Opening up the Spa")
    spa.start()
}