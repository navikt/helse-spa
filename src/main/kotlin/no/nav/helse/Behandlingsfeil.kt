package no.nav.helse

import no.nav.helse.behandling.Behandlingsgrunnlag
import no.nav.helse.behandling.Sakskompleks
import no.nav.helse.behandling.UavklarteFakta
import no.nav.helse.behandling.mvp.MVPFeil
import no.nav.helse.behandling.søknad.Sykepengesøknad

interface Behandlingsfeil {
    val sakskompleksId: String
    val feilmelding: String

    data class MVPFilterFeil(val sakskompleks: Sakskompleks, val mvpFeil: List<MVPFeil>, override val feilmelding: String, override val sakskompleksId: String = sakskompleks.id): Behandlingsfeil

    data class RegisterFeil(override val feilmelding: String, val throwable: Throwable, val sakskompleks: Sakskompleks, override val sakskompleksId: String = sakskompleks.id): Behandlingsfeil

    data class Avklaringsfeil(val uavklarteFakta: UavklarteFakta, override val feilmelding: String, override val sakskompleksId: String = uavklarteFakta.sakskompleks.id): Behandlingsfeil

    data class Vilkårsprøvingsfeil(val vilkårsprøving: Behandlingsgrunnlag, override val feilmelding: String, override val sakskompleksId: String = vilkårsprøving.sakskompleks.id): Behandlingsfeil

    data class Beregningsfeil(val vilkårsprøving: Behandlingsgrunnlag, override val feilmelding: String, override val sakskompleksId: String = vilkårsprøving.sakskompleks.id): Behandlingsfeil


    companion object {

        fun mvpFilter(sakskompleks: Sakskompleks, mvpFeil: List<MVPFeil>) = MVPFilterFeil(sakskompleks, mvpFeil, "Sakskompleks faller ut fordi det passer ikke for MVP")

        // vi klarte ikke avklare alle fakta
        fun avklaringsfeil(uavklarteFakta: UavklarteFakta) = Avklaringsfeil(uavklarteFakta, "Kunne ikke fastsette alle fakta.")

        // vi klarte ikke vilkårsprøve, eller vilkårsprøving feilet
        fun vilkårErIkkeOppfylt(vilkårsprøving: Behandlingsgrunnlag) = Vilkårsprøvingsfeil(vilkårsprøving, "Vilkår er ikke oppfylt.")

        // her feilet noe under _beregning, men vi har ikke del-resultat, bare exception
        fun beregningsfeil(vilkårsprøving: Behandlingsgrunnlag, exception: Exception) = Beregningsfeil(vilkårsprøving, "Beregning feilet: ${exception.javaClass.simpleName}: ${exception.message}.")

        fun registerFeil(exception: Throwable, sakskompleks: Sakskompleks):RegisterFeil = RegisterFeil("Feil i opphenting av register-data: ${exception.javaClass.simpleName} : ${exception.message}\"", exception, sakskompleks)

    }
}
