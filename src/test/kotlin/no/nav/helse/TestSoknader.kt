package no.nav.helse

import java.time.LocalDate
import java.time.LocalDateTime

val tpsFaktaUtenVerdi = Tpsfakta(
        fodselsdato = LocalDate.now(),
        bostedland = "bytt ut dette i tester"
)

val faktagrunnlagUtenVerdi = Faktagrunnlag(
        tps = tpsFaktaUtenVerdi
)

val originalSoknad = Sykepengesoknad(
        aktorId = "1",
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