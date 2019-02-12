package no.nav.helse

import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
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
import io.prometheus.client.CollectorRegistry
import no.nav.NarePrometheus
import no.nav.helse.sykepenger.beregning.Beregningsgrunnlag
import no.nav.helse.sykepenger.beregning.Beregningsresultat
import no.nav.helse.sykepenger.beregning.Sykepengegrunnlag
import no.nav.helse.sykepenger.beregning.beregn
import no.nav.helse.sykepenger.vilkar.Vilkårsgrunnlag
import no.nav.helse.sykepenger.vilkar.sykepengevilkår
import no.nav.nare.core.evaluations.Evaluering
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = LoggerFactory.getLogger("Spa")

fun main() {
    /*val spa = SaksbehandlingStream(Environment())
    log.info("Opening up the Spa")
    spa.start()*/
    val nare: NarePrometheus = NarePrometheus(CollectorRegistry.defaultRegistry)

    embeddedServer(Netty, 8181) {
        install(ContentNegotiation) {
            gson {
                setDateFormat("yyyy-mm-dd")
                registerTypeAdapter(LocalDateTime::class.java, JsonDeserializer<LocalDateTime> { json, type, context ->
                    LocalDateTime.parse(json.asString)
                })
                registerTypeAdapter(LocalDate::class.java, JsonDeserializer<LocalDate> { json, type, context ->
                    LocalDate.parse(json.asString)
                })
                registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, typeOfSrc, context ->
                    JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE))
                })
                registerTypeAdapter(Sykepengesøknad::class.java, JsonDeserializer<Sykepengesøknad> { json, type, context ->
                    Sykepengesøknad(
                            json.asJsonObject["aktorId"].asString,
                            json.asJsonObject["soktUtenlandsopphold"].asBoolean,
                            LocalDate.parse(json.asJsonObject["fom"].asString),
                            LocalDate.parse(json.asJsonObject["tom"].asString),
                            LocalDate.parse(json.asJsonObject["startSyketilfelle"].asString),
                            LocalDateTime.parse(json.asJsonObject["sendtNav"].asString),
                            json.asJsonObject["soknadsperioder"].asJsonArray.map {
                                Søknadsperiode(
                                        LocalDate.parse(it.asJsonObject["fom"].asString),
                                        LocalDate.parse(it.asJsonObject["tom"].asString),
                                        it.asJsonObject["sykmeldingsgrad"].asInt
                                )
                            },
                            Faktagrunnlag(
                                Tpsfakta(
                                        LocalDate.parse(json.asJsonObject["faktagrunnlag"].asJsonObject["tps"].asJsonObject["fodselsdato"].asString),
                                        json.asJsonObject["faktagrunnlag"].asJsonObject["tps"].asJsonObject["bostedland"].asString
                                ),
                                Aaregfakta(
                                        json.asJsonObject["faktagrunnlag"].asJsonObject["aareg"].asJsonObject["arbeidsforhold"].asJsonArray.map {
                                            Arbeidsforhold(
                                                    Arbeidsgiver(it.asJsonObject["arbeidsgiver"].asJsonObject["orgnummer"].asString),
                                                    it.asJsonObject["arbeidsavtale"].asJsonArray.map {
                                                        Arbeidsavtale(LocalDate.parse(it.asJsonObject["fomGyldighetsperiode"].asString))
                                                    }
                                            )
                                        }
                                ),
                                Aordningenfakta(json.asJsonObject["faktagrunnlag"].asJsonObject["aordningen"].asJsonObject["perioder"].asJsonArray.map {
                                    Periode(LocalDate.parse(it.asJsonObject["periode"].asString), it.asJsonObject["belop"].asLong)
                                }),
                                Inntektsmeldingfakta(json.asJsonObject["faktagrunnlag"].asJsonObject["inntektsmelding"].asJsonObject["belop"].asLong)
                            ),
                            json.asJsonObject["harVurdertInntekt"].asBoolean,
                            json.asJsonObject["andreYtelser"].asJsonArray.map {
                                it.asString
                            }
                    )
                })
            }
        }
        routing {
            routeResources()

            post("/api/beregn") {
                log.info("evaluating application")
                val søknad = call.receive<Sykepengesøknad>()
                log.info("Received $søknad")

                val vilkårsgrunnlag = Vilkårsgrunnlag(
                        førsteSykdomsdag = søknad.startSyketilfelle,
                        datoForAnsettelse = søknad.faktagrunnlag.aareg.arbeidsforhold[0].arbeidsavtale[0].fomGyldighetsperiode,
                        alder = søknad.faktagrunnlag.tps.alder(),
                        bostedlandISykdomsperiode = søknad.faktagrunnlag.tps.bostedland,
                        ytelser = søknad.andreYtelser,
                        søknadSendt = søknad.sendtNav.toLocalDate(),
                        førsteDagSøknadGjelderFor = søknad.fom,
                        aktuellMånedsinntekt = søknad.faktagrunnlag.inntektsmelding.beløp,
                        rapportertMånedsinntekt = søknad.faktagrunnlag.aordningen.perioder.sumBy {
                            it.beløp.toInt()
                        }.toLong(),
                        grunnbeløp = 96883,
                        harVurdertInntekt = søknad.harVurdertInntekt
                )

                val evaluering = nare.tellEvaluering { sykepengevilkår.evaluer(vilkårsgrunnlag) }

                val beregningsgrunnlag = Beregningsgrunnlag(
                        søknad.fom,
                        null,
                        null,
                        søknad.soknadsperioder[0].sykmeldingsgrad,
                        Sykepengegrunnlag(vilkårsgrunnlag.fastsattÅrsinntekt, vilkårsgrunnlag.grunnbeløp),
                        søknad.tom
                )
                val beregningsresultat = if (evaluering.resultat == no.nav.nare.core.evaluations.Resultat.JA) beregn(beregningsgrunnlag) else null

                this.call.respond(Resultat(evaluering, beregningsresultat))
            }
        }
    }.start(wait = false)
}

data class Resultat(val evaluering: Evaluering, val beregningsresultat: Beregningsresultat?)

fun Route.routeResources() {
    static("") {
        resources("css")
        resources("js")
        resources("html")
    }
}
