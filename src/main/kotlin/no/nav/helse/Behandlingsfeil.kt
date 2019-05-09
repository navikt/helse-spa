package no.nav.helse

import no.nav.helse.behandling.Behandlingsgrunnlag
import no.nav.helse.behandling.UavklarteFakta
import no.nav.helse.behandling.mvp.MVPFeil
import no.nav.helse.behandling.søknad.Sykepengesøknad

interface Behandlingsfeil {
    val soknadId: String
    val feilmelding: String

    data class MVPFilterFeil(val søknad: Sykepengesøknad, val mvpFeil: List<MVPFeil>, override val feilmelding: String, override val soknadId: String = søknad.id): Behandlingsfeil

    data class RegisterFeil(override val feilmelding: String, val throwable: Throwable, val søknad: Sykepengesøknad, override val soknadId: String = søknad.id): Behandlingsfeil

    data class Avklaringsfeil(val uavklarteFakta: UavklarteFakta, override val feilmelding: String, override val soknadId: String = uavklarteFakta.originalSøknad.id): Behandlingsfeil

    data class Vilkårsprøvingsfeil(val vilkårsprøving: Behandlingsgrunnlag, override val feilmelding: String, override val soknadId: String = vilkårsprøving.originalSøknad.id): Behandlingsfeil

    data class Beregningsfeil(val vilkårsprøving: Behandlingsgrunnlag, override val feilmelding: String, override val soknadId: String = vilkårsprøving.originalSøknad.id): Behandlingsfeil


    companion object {

        fun mvpFilter(søknad: Sykepengesøknad, mvpFeil: List<MVPFeil>) = MVPFilterFeil(søknad, mvpFeil, "Søknad faller ut fordi den passer ikke for MVP")

        // vi klarte ikke avklare alle fakta
        fun avklaringsfeil(uavklarteFakta: UavklarteFakta) = Avklaringsfeil(uavklarteFakta, "Kunne ikke fastsette alle fakta.")

        // vi klarte ikke vilkårsprøve, eller vilkårsprøving feilet
        fun vilkårErIkkeOppfylt(vilkårsprøving: Behandlingsgrunnlag) = Vilkårsprøvingsfeil(vilkårsprøving, "Vilkår er ikke oppfylt.")

        // her feilet noe under _beregning, men vi har ikke del-resultat, bare exception
        fun beregningsfeil(vilkårsprøving: Behandlingsgrunnlag, exception: Exception) = Beregningsfeil(vilkårsprøving, "Beregning feilet: ${exception.javaClass.simpleName}: ${exception.message}.")

        fun registerFeil(exception: Throwable, søknad: Sykepengesøknad):RegisterFeil = RegisterFeil("Feil i opphenting av register-data: ${exception.javaClass.simpleName} : ${exception.message}\"", exception, søknad)

    }
}
