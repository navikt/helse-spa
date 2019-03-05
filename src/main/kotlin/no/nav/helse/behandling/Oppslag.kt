package no.nav.helse.behandling

import no.nav.helse.Behandlingsfeil
import no.nav.helse.Either
import no.nav.helse.oppslag.ArbeidsforholdOppslag
import no.nav.helse.oppslag.Inntektsoppslag
import no.nav.helse.oppslag.PersonOppslag
import no.nav.helse.oppslag.StsRestClient

class Oppslag(val sparkelBaseUrl: String, val stsClient: StsRestClient) {
    fun hentRegisterData(søknad: Sykepengesøknad): Either<Behandlingsfeil, FaktagrunnlagResultat> =
            try {
                Either.Right(FaktagrunnlagResultat(
                        originalSøknad = søknad,
                        faktagrunnlag = Faktagrunnlag(
                                tps = PersonOppslag(sparkelBaseUrl, stsClient).hentTPSData(søknad),
                                beregningsperiode = Inntektsoppslag(sparkelBaseUrl, stsClient).hentBeregningsgrunnlag(søknad.aktorId, søknad.startSyketilfelle.minusMonths(3), søknad.startSyketilfelle.minusMonths(1)),
                                sammenligningsperiode = Inntektsoppslag(sparkelBaseUrl, stsClient).hentSammenligningsgrunnlag(søknad.aktorId, søknad.startSyketilfelle.minusYears(1), søknad.startSyketilfelle.minusMonths(1)),
                                sykepengeliste = emptyList(),
                                arbeidsforhold = ArbeidsforholdOppslag(sparkelBaseUrl, stsClient).hentArbeidsforhold(søknad))
                ))
            } catch(e: Exception) {
                Either.Left(Behandlingsfeil.registerFeil(e))
            }
}