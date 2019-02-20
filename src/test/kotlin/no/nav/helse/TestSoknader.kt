package no.nav.helse

import java.time.LocalDate
import java.time.LocalDateTime

val tpsFaktaUtenVerdi = Tpsfakta(
        fodselsdato = LocalDate.now(),
        bostedland = "bytt ut dette i tester"
)

val inntektFaktaUtenVerdi = InntektsFakta(
        arbeidsInntektIdentListe = emptyArray()
)

val arbeidsforholdFaktaUtenVerdi = ArbeidsforholdFakta(
        arbeidsgiverer = emptyList()
)

val faktagrunnlagUtenVerdi = Faktagrunnlag(
        tps = tpsFaktaUtenVerdi,
        inntekt = inntektFaktaUtenVerdi,
        arbeidsforhold = arbeidsforholdFaktaUtenVerdi
)

val originalSoknad = Sykepengesoknad(
        aktorId = "1",
        arbeidsgiver = Arbeidsgiver("Test 1", "1111"),
        startSyketilfelle = LocalDate.now(),
        soktUtenlandsopphold = true,
        soknadsperioder = emptyList(),
        sendtNav = LocalDateTime.now(),
        tom = LocalDate.now(),
        fom = LocalDate.now(),
        harVurdertInntekt = false
)

val soknadUtenVerdi = BeriketSykepengesøknad(
        originalSoknad = originalSoknad,
        faktagrunnlag = faktagrunnlagUtenVerdi
)

