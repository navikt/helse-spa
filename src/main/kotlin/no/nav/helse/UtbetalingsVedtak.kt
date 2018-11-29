package no.nav.helse

import java.math.BigDecimal
import java.time.LocalDate

sealed class Vedtak

/**
 * Forventet resultat av en søknad.
 *
 * Mulig at begrepet "vedtak" er belastet og burde erstattes av noe annet.
 */
data class UtbetalingsVedtak(
        /**utledes av søknad*/
        val søknadsnr: String,
        /**prosent-tall*/
        val gradering: Float,
        /**inklusiv start-dato for utbetaling. Dette vil typisk være første sykedag etter arbeidsgiverperioden, men det finnes unntak*/
        val fom: LocalDate,
        /**inklusiv siste dag for utbetaling. Dette vil være siste dag i sykemelding, siste dag i søknak, eller siste tilgjengelige antall utbetalingsdager overstiger brukerens tilgjengelige antall dager (som vanligvis er 248 dager etter arbeidsgiverperioden, ekslusive helger men inklusive helligdager)*/
        val tom: LocalDate,
        /**uavklart om dette er dagsats eller beløp for hele perioden*/
        val beløp: BigDecimal,
        /**typisk skal enten søker eller arbeidsgiver få utbetalingen*/
        val mottaker: String
) : Vedtak()

data class Avslagsvedtak(
        /**utledes av søknad*/
        val søknadsnr: String,
        /**utledes av NARE*/
        val årsak: String
) : Vedtak()

