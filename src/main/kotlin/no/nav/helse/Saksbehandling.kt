package no.nav.helse

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.helse.Behandlingsfeil.Companion.mvpFilter
import no.nav.helse.behandling.*
import no.nav.helse.behandling.mvp.MVPFeil
import no.nav.helse.behandling.søknad.Sykepengesøknad
import no.nav.helse.fastsetting.vurderFakta
import no.nav.helse.probe.SaksbehandlingProbe

fun Sykepengesøknad.behandle(oppslag: Oppslag, probe: SaksbehandlingProbe): Either<Behandlingsfeil, SykepengeVedtak> =
        mvpFilter().flatMap {
            hentRegisterData(oppslag)
        }.flatMap { faktagrunnlagResultat ->
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

fun Sykepengesøknad.mvpFilter() = when (type) {
    "ARBEIDSTAKERE" -> right()
    else -> mvpFilter(this, listOf(
            MVPFeil("Søknadstype - $type", "Søknaden er av feil type")
    )).left()
}

private fun Sykepengesøknad.hentRegisterData(oppslag: Oppslag): Either<Behandlingsfeil, FaktagrunnlagResultat> =
        oppslag.hentRegisterData(this)

private fun FaktagrunnlagResultat.fastsettFakta(): Either<Behandlingsfeil, AvklarteFakta> =
        vurderFakta(this)

private fun AvklarteFakta.prøvVilkår(probe: SaksbehandlingProbe): Either<Behandlingsfeil, Behandlingsgrunnlag> =
        vilkårsprøving(this, probe)

private fun Behandlingsgrunnlag.beregnSykepenger(): Either<Behandlingsfeil, Sykepengeberegning> =
        sykepengeBeregning(this)

private fun Sykepengeberegning.fattVedtak(): Either<Behandlingsfeil, SykepengeVedtak> =
        vedtak(this)
