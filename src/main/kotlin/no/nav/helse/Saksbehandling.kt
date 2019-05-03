package no.nav.helse

import arrow.core.Either
import arrow.core.flatMap
import no.nav.helse.behandling.*
import no.nav.helse.dto.SykepengesøknadV2DTO
import no.nav.helse.fastsetting.vurderFakta
import no.nav.helse.probe.SaksbehandlingProbe

fun SykepengesøknadV2DTO.behandle(oppslag: Oppslag, probe: SaksbehandlingProbe): Either<Behandlingsfeil, SykepengeVedtak> =
        this.mapToSykepengesøknad()
                .flatMap {
                    hentRegisterData(it, oppslag)
                }.flatMap {
                    mvpFilter(it)
                }.flatMap {
                    fastsettFakta(it)
                }.flatMap {
                    prøvVilkår(it, probe)
                }.flatMap {
                    beregnSykepenger(it)
                }.flatMap {
                    fattVedtak(it)
                }

private fun mvpFilter(fakta: FaktagrunnlagResultat): Either<Behandlingsfeil, FaktagrunnlagResultat> = fakta.mvpFilter()
private fun hentRegisterData(søknad: Sykepengesøknad, oppslag: Oppslag): Either<Behandlingsfeil, FaktagrunnlagResultat> = oppslag.hentRegisterData(søknad)
private fun fastsettFakta(fakta: FaktagrunnlagResultat): Either<Behandlingsfeil, AvklarteFakta> = vurderFakta(fakta)
private fun prøvVilkår(fakta: AvklarteFakta, probe: SaksbehandlingProbe): Either<Behandlingsfeil, Behandlingsgrunnlag> = vilkårsprøving(fakta, probe)
private fun beregnSykepenger(vilkårsprøving: Behandlingsgrunnlag): Either<Behandlingsfeil, Sykepengeberegning> = sykepengeBeregning(vilkårsprøving)
private fun fattVedtak(beregning: Sykepengeberegning): Either<Behandlingsfeil, SykepengeVedtak> = vedtak(beregning)
