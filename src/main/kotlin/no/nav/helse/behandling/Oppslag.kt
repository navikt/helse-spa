package no.nav.helse.behandling

import arrow.core.Either
import arrow.core.flatMap
import no.nav.helse.Behandlingsfeil
import no.nav.helse.oppslag.Inntektsoppslag
import no.nav.helse.oppslag.PersonOppslag
import no.nav.helse.oppslag.StsRestClient
import no.nav.helse.oppslag.SykepengehistorikkOppslag
import no.nav.helse.oppslag.arbeidinntektytelse.ArbeidInntektYtelseOppslag

class Oppslag(val sparkelBaseUrl: String, val stsClient: StsRestClient) {

    fun hentRegisterData(søknad: Sykepengesøknad): Either<Behandlingsfeil, FaktagrunnlagResultat> =
            with(søknad) {
                prøv(hentPerson()) deretter { tpsfakta ->
                    prøv(hentBeregningsgrunnlag()) deretter { beregningsperiode ->
                        prøv(hentSammenligningsgrunnlag()) deretter { sammenligningsperiode ->
                            prøv(hentArbeidInntektYtelse()) deretter { arbeidInntektYtelse ->
                                prøv(hentSykepengehistorikk()) deretter { sykepengehistorikk ->
                                    try {
                                        Either.Right(FaktagrunnlagResultat(
                                                originalSøknad = søknad,
                                                faktagrunnlag = Faktagrunnlag(
                                                        tps = tpsfakta,
                                                        beregningsperiode = beregningsperiode,
                                                        sammenligningsperiode = sammenligningsperiode,
                                                        sykepengehistorikk = sykepengehistorikk,
                                                        arbeidInntektYtelse = arbeidInntektYtelse
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


    private infix fun <T> Sykepengesøknad.prøv(either: Either<Exception, T>) = object {
        infix fun <U> deretter(hentMer: (t: T) -> Either<Behandlingsfeil, U>) = either.mapLeft { markerFeil(it) }.flatMap(hentMer)
    }

    private fun Sykepengesøknad.markerFeil(ex: Exception) = Behandlingsfeil.registerFeil(ex, this)
    private fun Sykepengesøknad.hentPerson() = PersonOppslag(sparkelBaseUrl, stsClient).hentTPSData(this)
    private fun Sykepengesøknad.hentBeregningsgrunnlag() = Inntektsoppslag(sparkelBaseUrl, stsClient).hentBeregningsgrunnlag(aktorId, arbeidsgiver.orgnummer, startSyketilfelle.minusMonths(3), startSyketilfelle.minusMonths(1))
    private fun Sykepengesøknad.hentSammenligningsgrunnlag() = Inntektsoppslag(sparkelBaseUrl, stsClient).hentSammenligningsgrunnlag(aktorId, startSyketilfelle.minusYears(1), startSyketilfelle.minusMonths(1))
    private fun Sykepengesøknad.hentArbeidInntektYtelse() = ArbeidInntektYtelseOppslag(sparkelBaseUrl, stsClient).hentArbeidInntektYtelse(this)
    private fun Sykepengesøknad.hentSykepengehistorikk() = SykepengehistorikkOppslag(sparkelBaseUrl, stsClient).hentSykepengehistorikk(aktorId, startSyketilfelle)
}
