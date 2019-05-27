package no.nav.helse.behandling

import no.nav.helse.Grunnlagsdata
import no.nav.helse.behandling.søknad.Sykepengesøknad
import no.nav.helse.fastsetting.*
import no.nav.helse.oppslag.AnvistPeriodeDTO
import no.nav.helse.oppslag.Inntekt
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsforholdDTO
import java.time.LocalDate

sealed class AvklaringsResultat

data class AvklarteFakta(
        val originalSøknad: Sykepengesøknad,
        val faktagrunnlag: Faktagrunnlag,
        val avklarteVerdier: AvklarteVerdier
) : AvklaringsResultat()

data class AvklarteVerdier(
        val medlemsskap: Vurdering.Avklart<Boolean, Tpsfakta>,
        val alder: Vurdering.Avklart<Alder, Aldersgrunnlag>,
        val maksdato: Vurdering.Avklart<LocalDate, Grunnlagsdata>,
        val sykepengehistorikk: List<AnvistPeriodeDTO>,
        val arbeidsforhold: Vurdering.Avklart<ArbeidsforholdDTO, List<ArbeidsforholdDTO>>,
        val opptjeningstid: Vurdering.Avklart<Opptjeningstid, Opptjeningsgrunnlag>,
        val sykepengegrunnlag: Vurdering.Avklart<Sykepengegrunnlag, List<Inntekt>>
)

data class UavklarteFakta(
        val originalSøknad: Sykepengesøknad,
        val faktagrunnlag: Faktagrunnlag,
        val uavklarteVerdier: UavklarteVerdier
) : AvklaringsResultat()
