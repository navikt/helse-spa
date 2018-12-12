package no.nav.helse

import kotlin.concurrent.thread

fun main() {
    val spa = SaksbehandlingStream(Environment())
    spa.start()
    Runtime.getRuntime().addShutdownHook(thread {
        spa.stop()
    })
}

