package no.nav.helse.behandling

import no.nav.helse.*
import no.nav.helse.oppslag.*

class Oppslag(val sparkelBaseUrl: String, val stsClient: StsRestClient) {

    fun hentRegisterData(søknad: Sykepengesøknad): Either<Behandlingsfeil, FaktagrunnlagResultat> =
            with(søknad) {
                prøv(hentPerson())                      deretter { tpsfakta ->
                prøv(hentBeregningsgrunnlag())          deretter { beregningsperiode ->
                prøv(hentSammenligningsgrunnlag())      deretter { sammenligningsperiode ->
                prøv(hentArbeidsforhold())              deretter { arbeidsforhold ->
                prøv(hentSykepengehistorikk())          deretter { sykepengehistorikk ->
                    try {
                        Either.Right(FaktagrunnlagResultat(
                                originalSøknad = søknad,
                                faktagrunnlag = Faktagrunnlag(
                                        tps = tpsfakta,
                                        beregningsperiode = beregningsperiode,
                                        sammenligningsperiode = sammenligningsperiode,
                                                        sykepengehistorikk = sykepengehistorikk,
                                        arbeidsforhold = arbeidsforhold
                                )))
                    } catch (e: Exception) {
                        Either.Left(Behandlingsfeil.registerFeil(e, søknad))
                    }
                }}}}}
            }


    private infix fun <T> Sykepengesøknad.prøv(either: Either<Exception, T>) = object {
        infix fun <U> deretter(hentMer: (t: T) -> Either<Behandlingsfeil, U>) = either.mapLeft { markerFeil(it) }.flatMap(hentMer)
    }

    private fun Sykepengesøknad.markerFeil(ex: Exception) = Behandlingsfeil.registerFeil(ex, this)
    private fun Sykepengesøknad.hentPerson() = PersonOppslag(sparkelBaseUrl, stsClient).hentTPSData(this)
    private fun Sykepengesøknad.hentBeregningsgrunnlag() = Inntektsoppslag(sparkelBaseUrl, stsClient).hentBeregningsgrunnlag(aktorId, arbeidsgiver.orgnummer, startSyketilfelle.minusMonths(3), startSyketilfelle.minusMonths(1))
    private fun Sykepengesøknad.hentSammenligningsgrunnlag() = Inntektsoppslag(sparkelBaseUrl, stsClient).hentSammenligningsgrunnlag(aktorId, startSyketilfelle.minusYears(1), startSyketilfelle.minusMonths(1))
    private fun Sykepengesøknad.hentArbeidsforhold() = ArbeidsforholdOppslag(sparkelBaseUrl, stsClient).hentArbeidsforhold(this)
    private fun Sykepengesøknad.hentSykepengehistorikk() = SykepengehistorikkOppslag(sparkelBaseUrl, stsClient).hentSykepengehistorikk(aktorId, startSyketilfelle)
}
