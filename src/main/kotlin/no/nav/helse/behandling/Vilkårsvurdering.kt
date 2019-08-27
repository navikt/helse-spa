package no.nav.helse.behandling

import arrow.core.Either
import no.nav.helse.Behandlingsfeil
import no.nav.helse.oppslag.getGrunnbeløpForDato
import no.nav.helse.probe.SaksbehandlingProbe
import no.nav.helse.sykepenger.vilkar.Vilkårsgrunnlag
import no.nav.helse.sykepenger.vilkar.sykepengevilkår
import no.nav.nare.core.evaluations.Resultat

fun vilkårsprøving(avklarteFakta: AvklarteFakta, probe: SaksbehandlingProbe): Either<Behandlingsfeil, Behandlingsgrunnlag> {
    val grunnlag = Vilkårsgrunnlag(
            opptjeningstid = avklarteFakta.avklarteVerdier.opptjeningstid.fastsattVerdi.toInt(),
            alder = avklarteFakta.avklarteVerdier.alder.fastsattVerdi,
            erMedlem = avklarteFakta.avklarteVerdier.medlemsskap.fastsattVerdi,
            ytelser = emptyList(),
            søknadSendt = avklarteFakta.sakskompleks.søknader[0].sendtNav!!.toLocalDate(),
            førsteDagSøknadGjelderFor = avklarteFakta.sakskompleks.søknader[0].fom,
            sisteDagSøknadenGjelderFor = avklarteFakta.sakskompleks.søknader[0].tom,
            sisteMuligeSykepengedag = avklarteFakta.avklarteVerdier.maksdato.fastsattVerdi,
            fastsattÅrsinntekt = avklarteFakta.avklarteVerdier.sykepengegrunnlag.fastsattVerdi.sykepengegrunnlagNårTrygdenYter.fastsattVerdi,
            grunnbeløp = getGrunnbeløpForDato(avklarteFakta.sakskompleks.søknader[0].fom)
    )

    val vilkårsprøving = sykepengevilkår.evaluer(grunnlag)

    probe.gjennomførtVilkårsprøving(vilkårsprøving)

    val behandlingsgrunnlag = Behandlingsgrunnlag(
            sakskompleks = avklarteFakta.sakskompleks,
            faktagrunnlag = avklarteFakta.faktagrunnlag,
            avklarteVerdier = avklarteFakta.avklarteVerdier,
            vilkårsprøving = vilkårsprøving
    )

    return when (behandlingsgrunnlag.vilkårsprøving.resultat) {
        Resultat.JA -> Either.Right(behandlingsgrunnlag)
        else -> Either.Left(Behandlingsfeil.vilkårErIkkeOppfylt(behandlingsgrunnlag))
    }
}

