package no.nav.helse.behandling

import no.nav.helse.Behandlingsfeil
import no.nav.helse.Either
import no.nav.helse.flatMap
import no.nav.helse.oppslag.ArbeidsforholdOppslag
import no.nav.helse.oppslag.Inntektsoppslag
import no.nav.helse.oppslag.PersonOppslag
import no.nav.helse.oppslag.StsRestClient

class Oppslag(val sparkelBaseUrl: String, val stsClient: StsRestClient) {
    fun hentRegisterData(eitherSøknadOrFail: Either<Behandlingsfeil, Sykepengesøknad>): Either<Behandlingsfeil, FaktagrunnlagResultat> =
            eitherSøknadOrFail.flatMap {
                Either.Right(FaktagrunnlagResultat(
                        originalSøknad = it,
                        faktagrunnlag = Faktagrunnlag(
                                tps = PersonOppslag(sparkelBaseUrl, stsClient).hentTPSData(it),
                                beregningsperiode = Inntektsoppslag(sparkelBaseUrl, stsClient).hentBeregningsgrunnlag(it.aktorId, it.startSyketilfelle.minusMonths(3), it.startSyketilfelle.minusMonths(1)),
                                sammenligningsperiode = Inntektsoppslag(sparkelBaseUrl, stsClient).hentSammenligningsgrunnlag(it.aktorId, it.startSyketilfelle.minusYears(1), it.startSyketilfelle.minusMonths(1)),
                                sykepengeliste = emptyList(),
                                arbeidsforhold = ArbeidsforholdOppslag(sparkelBaseUrl, stsClient).hentArbeidsforhold(it))
                )
                )
            }
}