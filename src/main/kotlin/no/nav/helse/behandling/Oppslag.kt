package no.nav.helse.behandling

import arrow.core.Either
import arrow.core.Try
import arrow.core.flatMap
import no.nav.helse.Behandlingsfeil
import no.nav.helse.oppslag.Inntektsoppslag
import no.nav.helse.oppslag.PersonOppslag
import no.nav.helse.oppslag.StsRestClient
import no.nav.helse.oppslag.SykepengehistorikkOppslag
import no.nav.helse.oppslag.arbeidinntektytelse.ArbeidInntektYtelseOppslag
import no.nav.helse.oppslag.arbeidinntektytelse.YtelserOppslag

class Oppslag(val sparkelBaseUrl: String, val stsClient: StsRestClient) {

    fun hentRegisterData(sakskompleks: Sakskompleks): Either<Behandlingsfeil, FaktagrunnlagResultat> =
            with(sakskompleks) {
                hentPerson().toEither { markerFeil(it) }.flatMap { tpsfakta ->
                    hentBeregningsgrunnlag().toEither { markerFeil(it) }.flatMap { beregningsperiode ->
                        hentSammenligningsgrunnlag().toEither { markerFeil(it) }.flatMap { sammenligningsperiode ->
                            hentArbeidInntektYtelse().toEither { markerFeil(it) }.flatMap { arbeidInntektYtelse ->
                                hentSykepengehistorikk().toEither { markerFeil(it) }.flatMap { sykepengehistorikk ->
                                    hentYtelser().toEither { markerFeil(it) }.flatMap { ytelser ->
                                        Try {
                                            FaktagrunnlagResultat(
                                                    sakskompleks = sakskompleks,
                                                    faktagrunnlag = Faktagrunnlag(
                                                            tps = tpsfakta,
                                                            beregningsperiode = beregningsperiode,
                                                            sammenligningsperiode = sammenligningsperiode,
                                                            sykepengehistorikk = sykepengehistorikk,
                                                            arbeidInntektYtelse = arbeidInntektYtelse,
                                                            ytelser = ytelser
                                                    ))
                                        }.toEither {
                                            Behandlingsfeil.registerFeil(it, sakskompleks)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

    private fun Sakskompleks.markerFeil(ex: Throwable) = Behandlingsfeil.registerFeil(ex, this)
    private fun Sakskompleks.hentPerson() = PersonOppslag(sparkelBaseUrl, stsClient).hentTPSData(this)
    private fun Sakskompleks.hentBeregningsgrunnlag() = Inntektsoppslag(sparkelBaseUrl, stsClient).hentBeregningsgrunnlag(aktørId, orgnummer!!, startSyketilfelle.minusMonths(3), startSyketilfelle.minusMonths(1))
    private fun Sakskompleks.hentSammenligningsgrunnlag() = Inntektsoppslag(sparkelBaseUrl, stsClient).hentSammenligningsgrunnlag(aktørId, startSyketilfelle.minusYears(1), startSyketilfelle.minusMonths(1))
    private fun Sakskompleks.hentArbeidInntektYtelse() = ArbeidInntektYtelseOppslag(sparkelBaseUrl, stsClient).hentArbeidInntektYtelse(this)
    private fun Sakskompleks.hentSykepengehistorikk() = SykepengehistorikkOppslag(sparkelBaseUrl, stsClient).hentSykepengehistorikk(aktørId, startSyketilfelle)
    private fun Sakskompleks.hentYtelser() = YtelserOppslag(sparkelBaseUrl, stsClient).hentYtelser(aktørId, startSyketilfelle.minusMonths(3), startSyketilfelle)
}
