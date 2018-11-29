package no.nav.helse

import no.nav.helse.vilkaar.harOpptjening
import no.nav.nare.evaluation.Evaluation
import no.nav.nare.evaluation.Result
import no.nav.nare.specifications.Specification
import java.math.BigDecimal
import java.time.LocalDate

/**
 * En fullstendig beriket sykepengesøknad
 */
data class Soknad(val søknadsNr: String,
                  val bruker: String, // AktørId
                  val arbeidsgiver: String, // Orgnummer eller aktørId?
                  val sykemeldingId: String, // identity reference
                  val sykemelding: Sykemelding,
                  val korrigertArbeidstid: Collection<KorrigertArbeidstid>,
                  val fravær: Collection<Fravær>,
                  val utdanningsgrad: Int,
                  val søktOmUtenlandsopphold: Boolean,
                  val annetSykefravær: Collection<Fravær>,
                  val andreInntektskilder: Collection<Inntektskilde>) {

    fun evaluer(): Vedtak {
        val harOpptjening: Specification<Soknad> = harOpptjening()
        val evaluate: Evaluation = harOpptjening.evaluate(this)

        return when (evaluate.result()) {
            Result.YES -> UtbetalingsVedtak(this.søknadsNr,
                    this.sykemelding.grad,
                    this.sykemelding.fom,
                    this.sykemelding.tom,
                    BigDecimal.TEN,
                    this.bruker
            )
            Result.NO -> Avslagsvedtak(this.søknadsNr,
                    "Mangler opptjening: ${evaluate.reason()}"
            )
        }
    }
}

data class Sykemelding(val grad: Float,
                       val fom: LocalDate,
                       val tom: LocalDate)

data class KorrigertArbeidstid(val fom: LocalDate,
                               val tom: LocalDate,
                               val faktiskGrad: Float,
                               val faktiskTimer: Int,
                               val avtaltTimer: Int)

data class Fravær(val fom: LocalDate,
                  val tom: LocalDate,
                  val type: Fraværstype)

enum class Fraværstype {
    FERIE, PERMISJON, UTDANNING, UTENLANDSOPPHOLD
}

data class Inntektskilde(val type: String,
                         val erSykemeldt: Boolean)