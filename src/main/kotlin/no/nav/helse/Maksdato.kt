package no.nav.helse

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import no.nav.helse.serde.defaultObjectMapper
import java.time.LocalDate

sealed class MaksdatoGrunnlag
data class TomtMaksdatoGrunnlag(val aarsak: String = "Venter på neste steg i fastsetting av fakta") : MaksdatoGrunnlag()
data class FaktiskMaksdatoGrunnlag(val request: MaksdatoSparkelRequest) : MaksdatoGrunnlag()

data class Tidsperiode(val fom: LocalDate, val tom: LocalDate)
data class MaksdatoSparkelRequest(
        val førsteFraværsdag: LocalDate,
        val førsteSykepengedag: LocalDate,
        val personensAlder: Int,
        val yrkesstatus: String = "ARBEIDSTAKER",
        val tidligerePerioder: List<Tidsperiode>)

class MaksdatoOppslag(val sparkelUrl: String, val stsRestClient: StsRestClient) {

    fun vurderMaksdato(soknad: AvklartSykepengesoknad): Vurdering<LocalDate, MaksdatoGrunnlag> {
        val alder  = soknad.alder
        return when(alder) {
            is Vurdering.Uavklart -> Vurdering.Uavklart(arsak = Vurdering.Uavklart.Arsak.MANGLENDE_DATA, begrunnelse = "Kan ikke fastsette maksdato for bruker med uavklart alder", grunnlag = TomtMaksdatoGrunnlag(aarsak = "Alder ikke avklart"))
            is Vurdering.Avklart -> {
                val request = MaksdatoSparkelRequest(
                        førsteFraværsdag = soknad.originalSoknad.startSyketilfelle,
                        førsteSykepengedag = soknad.originalSoknad.fom,
                        personensAlder = alder.fastsattVerdi,
                        yrkesstatus = "I literally don't know",
                        tidligerePerioder = soknad.sykepengeliste.map { Tidsperiode(fom = it.fom, tom = it.tom) }
                )
                val bearer = stsRestClient.token()

                val (_, _, result) = "$sparkelUrl/maksdato".httpPost()
                        .header(mapOf(
                                "Authorization" to "Bearer $bearer",
                                "Accept" to "application/json",
                                "Nav-Call-Id" to "anything",
                                "Nav-Consumer-Id" to "spa"
                        ))
                        .body(defaultObjectMapper.writeValueAsBytes(request))
                        .responseString()

                val maksdato = defaultObjectMapper.readValue(result.component1(), LocalDate::class.java)
                Vurdering.Avklart(fastsattVerdi = maksdato, fastsattAv = "SPA", grunnlag = FaktiskMaksdatoGrunnlag(request), begrunnelse = "")
            }
        }
}



}