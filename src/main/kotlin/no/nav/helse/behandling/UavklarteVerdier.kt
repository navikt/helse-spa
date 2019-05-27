package no.nav.helse.behandling

import no.nav.helse.Grunnlagsdata
import no.nav.helse.fastsetting.*
import no.nav.helse.oppslag.AnvistPeriodeDTO
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsforholdDTO
import java.time.LocalDate

data class UavklarteVerdier(
        val medlemsskap: Vurdering<Boolean, Tpsfakta>,
        val alder: Vurdering<Alder, Aldersgrunnlag>,
        val maksdato: Vurdering<LocalDate, Grunnlagsdata?>,
        val sykepengehistorikk: List<AnvistPeriodeDTO>,
        val arbeidsforhold: Vurdering<ArbeidsforholdDTO, List<ArbeidsforholdDTO>>,
        val opptjeningstid: Vurdering<Opptjeningstid, Opptjeningsgrunnlag>,
        val sykepengegrunnlag: Vurdering<*, *>
) {
    fun asNamedList(): List<Pair<String, Vurdering<*, *>>> = listOf(
            Pair("medlemsskap", medlemsskap),
            Pair("alder", alder),
            Pair("maksdato", maksdato),
            Pair("arbeidsforhold", arbeidsforhold),
            Pair("opptjeningstid", opptjeningstid),
            Pair("sykepengegrunnlag", sykepengegrunnlag)
    )
}
