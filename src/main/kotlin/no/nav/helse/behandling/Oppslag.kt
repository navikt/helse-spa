package no.nav.helse.behandling

import no.nav.helse.Behandlingsfeil
import no.nav.helse.Either
import no.nav.helse.flatMap
import no.nav.helse.mapLeft
import no.nav.helse.oppslag.*

class Oppslag(val sparkelBaseUrl: String, val stsClient: StsRestClient) {

    fun hentRegisterData(søknad: Sykepengesøknad): Either<Behandlingsfeil, FaktagrunnlagResultat> =
            with(søknad) {
                hentPerson().mapLeft {
                    markerFeil(it)
                }.flatMap { tpsfakta ->
                    hentBeregningsgrunnlag().mapLeft {
                        markerFeil(it)
                    }.flatMap { beregningsperiode ->
                        hentSammenligningsgrunnlag().mapLeft {
                            markerFeil(it)
                        }.flatMap { sammenligningsperiode ->
                            hentArbeidsforhold().mapLeft {
                                markerFeil(it)
                            }.flatMap { arbeidsforhold ->
                                hentInfotrygdBeregningsgrunnlag().mapLeft {
                                    markerFeil(it)
                                }.flatMap { infotrygdBeregningsgrunnlag ->
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

    private fun Sykepengesøknad.markerFeil(ex: Exception) = Behandlingsfeil.registerFeil(ex, this)
    private fun Sykepengesøknad.hentPerson() = PersonOppslag(sparkelBaseUrl, stsClient).hentTPSData(this)
    private fun Sykepengesøknad.hentBeregningsgrunnlag() = Inntektsoppslag(sparkelBaseUrl, stsClient).hentBeregningsgrunnlag(aktorId, arbeidsgiver.orgnummer, startSyketilfelle.minusMonths(3), startSyketilfelle.minusMonths(1))
    private fun Sykepengesøknad.hentSammenligningsgrunnlag() = Inntektsoppslag(sparkelBaseUrl, stsClient).hentSammenligningsgrunnlag(aktorId, startSyketilfelle.minusYears(1), startSyketilfelle.minusMonths(1))
    private fun Sykepengesøknad.hentArbeidsforhold() = ArbeidsforholdOppslag(sparkelBaseUrl, stsClient).hentArbeidsforhold(this)
    private fun Sykepengesøknad.hentInfotrygdBeregningsgrunnlag() = InfotrygdBeregningsgrunnlagOppslag(sparkelBaseUrl, stsClient).hentInfotrygdBeregningsgrunnlag(aktorId, startSyketilfelle)
}
