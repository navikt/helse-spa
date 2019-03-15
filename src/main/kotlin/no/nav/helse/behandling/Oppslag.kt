package no.nav.helse.behandling

import no.nav.helse.Behandlingsfeil
import no.nav.helse.Either
import no.nav.helse.flatMap
import no.nav.helse.mapLeft
import no.nav.helse.oppslag.*
import java.time.LocalDate

class Oppslag(val sparkelBaseUrl: String, val stsClient: StsRestClient) {

    fun hentRegisterData(søknad: Sykepengesøknad): Either<Behandlingsfeil, FaktagrunnlagResultat> =
            PersonOppslag(sparkelBaseUrl, stsClient).hentTPSData(søknad).mapLeft {
                Behandlingsfeil.registerFeil(it)
            }.flatMap { tpsfakta ->
                Inntektsoppslag(sparkelBaseUrl, stsClient).hentBeregningsgrunnlag(søknad.aktorId, søknad.startSyketilfelle.minusMonths(3), søknad.startSyketilfelle.minusMonths(1)).mapLeft {
                    Behandlingsfeil.registerFeil(it)
                }.flatMap { beregningsperiode ->
                    Inntektsoppslag(sparkelBaseUrl, stsClient).hentSammenligningsgrunnlag(søknad.aktorId, søknad.startSyketilfelle.minusYears(1), søknad.startSyketilfelle.minusMonths(1)).mapLeft {
                        Behandlingsfeil.registerFeil(it)
                    }.flatMap { sammenligningsperiode ->
                        ArbeidsforholdOppslag(sparkelBaseUrl, stsClient).hentArbeidsforhold(søknad).mapLeft {
                            Behandlingsfeil.registerFeil(it)
                        }.flatMap { arbeidsforhold ->
                            SykepengelisteOppslag(sparkelBaseUrl, stsClient).hentSykepengeliste(søknad.aktorId, søknad.startSyketilfelle).mapLeft {
                                Behandlingsfeil.registerFeil(it)
                            }.flatMap { sykepengeperioder ->
                                try {
                                    Either.Right(FaktagrunnlagResultat(
                                            originalSøknad = søknad,
                                            faktagrunnlag = Faktagrunnlag(
                                                    tps = tpsfakta,
                                                    beregningsperiode = beregningsperiode,
                                                    sammenligningsperiode = sammenligningsperiode,
                                                    sykepengeliste = sykepengeperioder,
                                                    arbeidsforhold = arbeidsforhold
                                            )))
                                } catch (e: Exception) {
                                    Either.Left(Behandlingsfeil.registerFeil(e))
                                }
                            }
                        }
                    }
                }
            }
}
