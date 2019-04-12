package no.nav.helse

import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("Spa")

fun main() {
    val spa = SaksbehandlingStream(Environment())
    log.info("The Spa is open for E-Business")
    spa.start()
}
