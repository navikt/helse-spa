package no.nav.helse.behandling

import no.nav.helse.*
import no.nav.helse.oppslag.*

class Oppslag(val sparkelBaseUrl: String, val stsClient: StsRestClient) {

    fun hentRegisterData(søknad: Sykepengesøknad): Either<Behandlingsfeil, FaktagrunnlagResultat> =
            with(søknad) {
                hentPerson().ellerDø(this) { tpsfakta ->
                    hentBeregningsgrunnlag().ellerDø(this) { beregningsperiode ->
                        hentSammenligningsgrunnlag().ellerDø(this) { sammenligningsperiode ->
                            hentArbeidsforhold().ellerDø(this) { arbeidsforhold ->
                                hentInfotrygdBeregningsgrunnlag().ellerDø(this) { infotrygdBeregningsgrunnlag ->
                                    try {
                                        Either.Right(FaktagrunnlagResultat(
                                                originalSøknad = søknad,
                                                faktagrunnlag = Faktagrunnlag(
                                                        tps = tpsfakta,
                                                        beregningsperiode = beregningsperiode,
                                                        sammenligningsperiode = sammenligningsperiode,
                                                        sykepengeliste = infotrygdBeregningsgrunnlag.sykepengerListe,
                                                        arbeidsforhold = arbeidsforhold
                                                )))
                                    } catch (e: Exception) {
                                        Either.Left(Behandlingsfeil.registerFeil(e, søknad))
                                    }
                                }
                            }
                        }
                    }
                }
            }

    private fun <T, U> Either<Exception, T>.ellerDø(søknad: Sykepengesøknad, hentMer: (t: T) -> Either<Behandlingsfeil, U>) = mapLeft {
        søknad.markerFeil(it)
    }.flatMap { hentMer(it) }

    private fun Sykepengesøknad.markerFeil(ex: Exception) = Behandlingsfeil.registerFeil(ex, this)
    private fun Sykepengesøknad.hentPerson() = PersonOppslag(sparkelBaseUrl, stsClient).hentTPSData(this)
    private fun Sykepengesøknad.hentBeregningsgrunnlag() = Inntektsoppslag(sparkelBaseUrl, stsClient).hentBeregningsgrunnlag(aktorId, arbeidsgiver.orgnummer, startSyketilfelle.minusMonths(3), startSyketilfelle.minusMonths(1))
    private fun Sykepengesøknad.hentSammenligningsgrunnlag() = Inntektsoppslag(sparkelBaseUrl, stsClient).hentSammenligningsgrunnlag(aktorId, startSyketilfelle.minusYears(1), startSyketilfelle.minusMonths(1))
    private fun Sykepengesøknad.hentArbeidsforhold() = ArbeidsforholdOppslag(sparkelBaseUrl, stsClient).hentArbeidsforhold(this)
    private fun Sykepengesøknad.hentInfotrygdBeregningsgrunnlag() = InfotrygdBeregningsgrunnlagOppslag(sparkelBaseUrl, stsClient).hentInfotrygdBeregningsgrunnlag(aktorId, startSyketilfelle)
}
