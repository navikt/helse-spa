package no.nav.helse.fastsetting

import no.nav.helse.ArbeidsgiverFakta
import java.time.LocalDate

typealias Opptjeningstid = Long

data class Opptjeningsgrunnlag(val førsteSykdomsdag: LocalDate, val arbeidsforhold: List<ArbeidsgiverFakta>)

fun vurderOpptjeningstid(opptjeningsgrunnlag: Opptjeningsgrunnlag): Vurdering<Opptjeningstid, Opptjeningsgrunnlag> =
    if (opptjeningsgrunnlag.arbeidsforhold.size != 1) {
        Vurdering.Uavklart(Vurdering.Uavklart.Årsak.FALLER_UTENFOR_MVP, "Søker har ${opptjeningsgrunnlag.arbeidsforhold.size} arbeidsforhold og vi forventer kun 1", opptjeningsgrunnlag)
    } else {
        opptjeningsgrunnlag.arbeidsforhold[0].sluttdato?.let {
            Vurdering.Uavklart<Opptjeningstid, Opptjeningsgrunnlag>(Vurdering.Uavklart.Årsak.FALLER_UTENFOR_MVP, "Søker har ett arbeidsforhold som han eller hun har avsluttet", opptjeningsgrunnlag)
        } ?: Vurdering.Avklart(opptjeningsgrunnlag.arbeidsforhold[0].startdato.datesUntil(opptjeningsgrunnlag.førsteSykdomsdag).count(), "Søker er i et aktivt arbeidsforhold", opptjeningsgrunnlag, "SPA")
    }
