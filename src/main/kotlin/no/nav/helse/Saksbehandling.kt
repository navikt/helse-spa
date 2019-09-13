package no.nav.helse

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.helse.Behandlingsfeil.Companion.mvpFilter
import no.nav.helse.behandling.AvklarteFakta
import no.nav.helse.behandling.Behandlingsgrunnlag
import no.nav.helse.behandling.FaktagrunnlagResultat
import no.nav.helse.behandling.Oppslag
import no.nav.helse.behandling.Sakskompleks
import no.nav.helse.behandling.SykepengeVedtak
import no.nav.helse.behandling.Sykepengeberegning
import no.nav.helse.behandling.mvp.MVPFeil
import no.nav.helse.behandling.mvpFilter
import no.nav.helse.behandling.sykepengeBeregning
import no.nav.helse.behandling.vedtak
import no.nav.helse.behandling.vilkårsprøving
import no.nav.helse.fastsetting.vurderFakta
import no.nav.helse.probe.SaksbehandlingProbe

fun Sakskompleks.behandle(oppslag: Oppslag, probe: SaksbehandlingProbe): Either<Behandlingsfeil, SykepengeVedtak> =
    mvpFilter().flatMap {
        hentRegisterData(oppslag)
    }
        .flatMap { faktagrunnlagResultat ->
            faktagrunnlagResultat.mvpFilter()
        }.flatMap { faktagrunnlagResultat ->
            faktagrunnlagResultat.fastsettFakta()
        }.flatMap { avklarteFakta ->
            avklarteFakta.prøvVilkår(probe)
        }.flatMap { behandlingsgrunnlag ->
            behandlingsgrunnlag.beregnSykepenger()
        }.flatMap { sykepengeberegning ->
            sykepengeberegning.fattVedtak()
        }


fun Sakskompleks.mvpFilter(): Either<Behandlingsfeil.MVPFilterFeil, Sakskompleks> {
    return when {
        søknader.isEmpty() ->
            mvpFilter(
                this, listOf(
                    MVPFeil(
                        "Ingen søknader",
                        "Sakskompleks inneholder ingen søknader"
                    )
                )
            ).left()

        søknader.size != 1 ->
            mvpFilter(
                this, listOf(
                    MVPFeil(
                        "Flere søknader",
                        "Sakskompleks inneholder mer enn én søknad (${søknader.size})"
                    )
                )
            ).left()

        søknader[0].type != "ARBEIDSTAKERE" ->
            mvpFilter(
                this, listOf(
                    MVPFeil("Søknadstype - ${søknader[0].type}", "Søknaden er av feil type")
                )
            ).left()

        else -> right()
    }
}

private fun Sakskompleks.hentRegisterData(oppslag: Oppslag): Either<Behandlingsfeil, FaktagrunnlagResultat> =
    oppslag.hentRegisterData(this)

private fun FaktagrunnlagResultat.fastsettFakta(): Either<Behandlingsfeil, AvklarteFakta> =
    vurderFakta(this)

private fun AvklarteFakta.prøvVilkår(probe: SaksbehandlingProbe): Either<Behandlingsfeil, Behandlingsgrunnlag> =
    vilkårsprøving(this, probe)

private fun Behandlingsgrunnlag.beregnSykepenger(): Either<Behandlingsfeil, Sykepengeberegning> =
    sykepengeBeregning(this)

private fun Sykepengeberegning.fattVedtak(): Either<Behandlingsfeil, SykepengeVedtak> =
    vedtak(this)
