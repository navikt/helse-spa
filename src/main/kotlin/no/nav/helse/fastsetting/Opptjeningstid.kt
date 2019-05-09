package no.nav.helse.fastsetting

import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsforholdDTO
import java.time.LocalDate

typealias Opptjeningstid = Long

data class Opptjeningsgrunnlag(val førsteSykdomsdag: LocalDate, val arbeidsforhold: Vurdering<ArbeidsforholdDTO, List<ArbeidsforholdDTO>>)

val begrunnelse_søker_i_aktivt_arbeidsforhold = "Søker er i et aktivt arbeidsforhold"

fun vurderOpptjeningstid(opptjeningsgrunnlag: Opptjeningsgrunnlag): Vurdering<Opptjeningstid, Opptjeningsgrunnlag> =
    when (opptjeningsgrunnlag.arbeidsforhold) {
        is Vurdering.Uavklart -> Vurdering.Uavklart(Vurdering.Uavklart.Årsak.HAR_IKKE_DATA, "Arbeidsforhold er ikke avklart", "Kan ikke avklare opptjeningstid når arbeidsforholdet ikke er avklart", opptjeningsgrunnlag)
        is Vurdering.Avklart -> Vurdering.Avklart(opptjeningsgrunnlag.arbeidsforhold.fastsattVerdi.startdato.datesUntil(opptjeningsgrunnlag.førsteSykdomsdag).count(), begrunnelse_søker_i_aktivt_arbeidsforhold, opptjeningsgrunnlag, "SPA")
    }
