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
                  val andreInntektskilder: Collection<Inntektskilde>,
                  val opptjeningstid: Collection<Opptjeningstid>) {

    fun evaluer(): Vedtak {
        val harOpptjening: Specification<Soknad> = harOpptjening()
        val evaluation: Evaluation = harOpptjening.evaluate(this)

        return when (evaluation.result()) {
            Result.YES -> UtbetalingsVedtak(this.søknadsNr,
                    this.sykemelding.grad,
                    this.sykemelding.fom,
                    this.sykemelding.tom,
                    BigDecimal.TEN,
                    this.bruker,
                    evaluation
            )
            Result.NO -> Avslagsvedtak(this.søknadsNr,
                    "Mangler opptjening: ${evaluation.reason()}",
                    evaluation
            )
        }
    }
}

data class Opptjeningstid(val fom: LocalDate,
                          val tom: LocalDate,
                          val type: Opptjeningstype)

enum class Opptjeningstype {
    JOBB, DAGPENGER, SYKEPENGER, PLEIEPENGER, FORELDREPENGER, ANNET
}

data class Sykemelding(val grad: Float,
                       val fom: LocalDate,
                       val tom: LocalDate)

/**
 * Når brukeren beskriver en arbeidstid som skiller seg fra arbeidstiden slik
 * den er oppgitt i sykemeldningen
 */
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