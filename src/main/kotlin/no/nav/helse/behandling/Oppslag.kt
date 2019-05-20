package no.nav.helse.behandling

import arrow.core.Either
import arrow.core.Try
import arrow.core.flatMap
import no.nav.helse.Behandlingsfeil
import no.nav.helse.behandling.søknad.Sykepengesøknad
import no.nav.helse.oppslag.Inntektsoppslag
import no.nav.helse.oppslag.PersonOppslag
import no.nav.helse.oppslag.StsRestClient
import no.nav.helse.oppslag.SykepengehistorikkOppslag
import no.nav.helse.oppslag.arbeidinntektytelse.ArbeidInntektYtelseOppslag
import no.nav.helse.oppslag.arbeidinntektytelse.YtelserOppslag

class Oppslag(val sparkelBaseUrl: String, val stsClient: StsRestClient) {

    fun hentRegisterData(søknad: Sykepengesøknad): Either<Behandlingsfeil, FaktagrunnlagResultat> =
            with(søknad) {
                hentPerson().toEither { markerFeil(it) }.flatMap { tpsfakta ->
                    hentBeregningsgrunnlag().toEither { markerFeil(it) }.flatMap { beregningsperiode ->
                        hentSammenligningsgrunnlag().toEither { markerFeil(it) }.flatMap { sammenligningsperiode ->
                            hentArbeidInntektYtelse().toEither { markerFeil(it) }.flatMap { arbeidInntektYtelse ->
                                hentSykepengehistorikk().toEither { markerFeil(it) }.flatMap { sykepengehistorikk ->
                                    hentYtelser().toEither { markerFeil(it) }.flatMap {
                                        Try {
                                            FaktagrunnlagResultat(
                                                    originalSøknad = søknad,
                                                    faktagrunnlag = Faktagrunnlag(
                                                            tps = tpsfakta,
                                                            beregningsperiode = beregningsperiode,
                                                            sammenligningsperiode = sammenligningsperiode,
                                                            sykepengehistorikk = sykepengehistorikk,
                                                            arbeidInntektYtelse = arbeidInntektYtelse
                                                    ))
                                        }.toEither {
                                            Behandlingsfeil.registerFeil(it, søknad)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

    private fun Sykepengesøknad.markerFeil(ex: Throwable) = Behandlingsfeil.registerFeil(ex, this)
    private fun Sykepengesøknad.hentPerson() = PersonOppslag(sparkelBaseUrl, stsClient).hentTPSData(this)
    private fun Sykepengesøknad.hentBeregningsgrunnlag() = Inntektsoppslag(sparkelBaseUrl, stsClient).hentBeregningsgrunnlag(aktorId, arbeidsgiver.orgnummer, startSyketilfelle.minusMonths(3), startSyketilfelle.minusMonths(1))
    private fun Sykepengesøknad.hentSammenligningsgrunnlag() = Inntektsoppslag(sparkelBaseUrl, stsClient).hentSammenligningsgrunnlag(aktorId, startSyketilfelle.minusYears(1), startSyketilfelle.minusMonths(1))
    private fun Sykepengesøknad.hentArbeidInntektYtelse() = ArbeidInntektYtelseOppslag(sparkelBaseUrl, stsClient).hentArbeidInntektYtelse(this)
    private fun Sykepengesøknad.hentSykepengehistorikk() = SykepengehistorikkOppslag(sparkelBaseUrl, stsClient).hentSykepengehistorikk(aktorId, startSyketilfelle)
    private fun Sykepengesøknad.hentYtelser() = YtelserOppslag(sparkelBaseUrl, stsClient).hentYtelser(aktorId, startSyketilfelle.minusMonths(3), startSyketilfelle)
}
