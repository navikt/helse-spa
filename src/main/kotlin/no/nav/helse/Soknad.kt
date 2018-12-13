package no.nav.helse

import no.nav.helse.vilkaar.harOpptjening
import no.nav.nare.evaluation.Evaluation
import no.nav.nare.specifications.Specification
import java.math.BigDecimal
import java.time.LocalDate

/**
 * En fullstendig beriket sykepengesøknad
 */
data class Soknad(val id: String, // antagelig en uuid?
                  val aktorId: String?, // AktørId
                  val sykemeldingId: String?, // identity reference
                  val soknadstype: String?,
                  val innsendtDato: LocalDate?,
                  val tom: LocalDate?,
                  val fom: LocalDate?,
                  val opprettetDato: LocalDate?,
                  val status: String

) {

    fun evaluer(): Vedtak {
        val harOpptjening: Specification<Soknad> = harOpptjening()
        val evaluation: Evaluation = harOpptjening.evaluate(this)

        /*
        return when (evaluation.result()) {
            Result.YES -> UtbetalingsVedtak(this.id,
                    this.sykemelding.grad,
                    this.sykemelding.fom,
                    this.sykemelding.tom,
                    BigDecimal.TEN,
                    this.aktorId,
                    evaluation
            )
            Result.NO -> Avslagsvedtak(this.id,
                    "Mangler opptjening: ${evaluation.reason()}",
                    evaluation
            )
        }
        */
        return UtbetalingsVedtak(this.id,
                100.0f,
                this.fom?: LocalDate.now(),
                this.tom?: LocalDate.now(),
                BigDecimal.valueOf(1000L),
                this.aktorId,
                evaluation
        )
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