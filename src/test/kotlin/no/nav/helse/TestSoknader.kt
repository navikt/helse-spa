package no.nav.helse

import no.nav.helse.behandling.Faktagrunnlag
import no.nav.helse.behandling.FaktagrunnlagResultat
import no.nav.helse.behandling.Sykepengesøknad
import no.nav.helse.behandling.Tpsfakta
import no.nav.helse.domain.Arbeidsforhold
import no.nav.helse.domain.Arbeidsgiver
import java.time.LocalDate
import java.time.LocalDateTime

val tpsFaktaUtenVerdi = Tpsfakta(
        fodselsdato = LocalDate.now(),
        bostedland = "bytt ut dette i tester"
)

val arbeidsforholdUtenVerdi = emptyList<Arbeidsforhold>()

val faktagrunnlagUtenVerdi = Faktagrunnlag(
        tps = tpsFaktaUtenVerdi,
        beregningsperiode = emptyList(),
        sammenligningsperiode = emptyList(),
        sykepengeliste = emptyList(),
        arbeidsforhold = arbeidsforholdUtenVerdi
)

val originalSoknad = Sykepengesøknad(
        id = "1",
        aktorId = "1",
        type = "ARBEIDSTAKERE",
        arbeidsgiver = Arbeidsgiver("Test 1", "1111"),
        startSyketilfelle = LocalDate.now(),
        soktUtenlandsopphold = true,
        soknadsperioder = emptyList(),
        sendtNav = LocalDateTime.now(),
        tom = LocalDate.now(),
        fom = LocalDate.now(),
        status = "SENDT"
)

val soknadUtenVerdi = FaktagrunnlagResultat(
        originalSøknad = originalSoknad,
        faktagrunnlag = faktagrunnlagUtenVerdi
)

