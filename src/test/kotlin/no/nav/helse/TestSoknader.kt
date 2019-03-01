package no.nav.helse

import no.nav.helse.behandling.*
import no.nav.helse.oppslag.*
import java.time.LocalDate
import java.time.LocalDateTime

val tpsFaktaUtenVerdi = Tpsfakta(
        fodselsdato = LocalDate.now(),
        bostedland = "bytt ut dette i tester"
)

val arbeidsforholdFaktaUtenVerdi = ArbeidsforholdFakta(
        arbeidsgivere = emptyList()
)

val faktagrunnlagUtenVerdi = Faktagrunnlag(
        tps = tpsFaktaUtenVerdi,
        beregningsperiode = emptyList(),
        sammenligningsperiode = emptyList(),
        sykepengeliste = emptyList(),
        arbeidsforhold = arbeidsforholdFaktaUtenVerdi
)

val originalSoknad = Sykepengesøknad(
        aktorId = "1",
        arbeidsgiver = Arbeidsgiver("Test 1", "1111"),
        startSyketilfelle = LocalDate.now(),
        soktUtenlandsopphold = true,
        soknadsperioder = emptyList(),
        sendtNav = LocalDateTime.now(),
        tom = LocalDate.now(),
        fom = LocalDate.now(),
        harVurdertInntekt = false,
        status = "SENDT"
)

val soknadUtenVerdi = FaktagrunnlagResultat(
        originalSøknad = originalSoknad,
        faktagrunnlag = faktagrunnlagUtenVerdi
)

