package no.nav.helse

import com.google.gson.Gson
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.helse.sykepenger.Søknad
import no.nav.helse.sykepenger.inngangsvilkår
import no.nav.nare.core.evaluations.Evaluering
import org.slf4j.LoggerFactory
import java.text.DateFormat
import java.time.LocalDate

private val log = LoggerFactory.getLogger("Spa")

fun main() {
    val spa = SaksbehandlingStream(Environment())
    log.info("Opening up the Spa")
    spa.start()

    embeddedServer(Netty, 80) {
        install(ContentNegotiation) {
            gson {
                setDateFormat("yyyy-mm-dd")
            }
        }
        routing {
            routeResources()
            post("/soknad") {
                log.info("evaluating application")
                val soknad = call.receive<SoknadFraSketch>()
                log.info("Received $soknad")
                this.call.respond(inngangsvilkår.evaluer(Søknad(
                        førsteSykdomsdag = soknad.forste_sykdomsdag ?: LocalDate.now(),
                        datoForAnsettelse = soknad.ansettelsesdato ?: LocalDate.now(),
                        alder = 50,
                        bostedlandISykdomsperiode = soknad.bosted ?: "Aardvark",
                        ytelser = parseAndreYtelser(soknad.andre_ytelser),
                        søknadSendt = soknad.soknad_sendt ?: LocalDate.now().minusYears(1000),
                        førsteDagSøknadGjelderFor = LocalDate.now(),
                        aktuellMånedsinntekt = 20000,
                        rapportertMånedsinntekt = 20000,
                        fastsattÅrsinntekt = 240000,
                        grunnbeløp = 100000,
                        harVurdertInntekt = false
                )).asJson())
            }
        }
    }.start(wait = false)
}

fun parseAndreYtelser(andreYtelser: String?): List<String> {
    return when(andreYtelser) {
        null -> emptyList()
        else -> listOf(andreYtelser)
    }
}

private fun Evaluering.asJson(): String {
    return Gson().toJson(this)
}

fun Route.routeResources() {
    static("") {
        resources("css")
        resources("js")
        resources("html")
    }
}