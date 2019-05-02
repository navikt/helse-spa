package no.nav.helse

import no.nav.helse.behandling.*
import no.nav.helse.domain.Arbeidsforhold
import no.nav.helse.domain.ArbeidsgiverFraSøknad
import no.nav.helse.fastsetting.*
import no.nav.helse.sykepenger.beregning.Beregningsresultat
import no.nav.helse.sykepenger.beregning.Dagsats
import no.nav.nare.core.evaluations.Evaluering
import java.time.LocalDate
import java.time.LocalDateTime

val tpsFaktaUtenVerdi = Tpsfakta(
        fodselsdato = LocalDate.now(),
        bostedland = "bytt ut dette i tester",
        statsborgerskap = "bytt ut dette i tester",
        status = "bytt ut dette i tester",
        diskresjonskode = "bytt ut dette i tester"
)

val arbeidsforholdUtenVerdi = emptyList<Arbeidsforhold>()

val faktagrunnlagUtenVerdi = Faktagrunnlag(
        tps = tpsFaktaUtenVerdi,
        beregningsperiode = emptyList(),
        sammenligningsperiode = emptyList(),
        sykepengehistorikk = emptyList(),
        arbeidsforhold = arbeidsforholdUtenVerdi
)

val originalSoknad = Sykepengesøknad(
        id = "1",
        aktorId = "1",
        type = "ARBEIDSTAKERE",
        arbeidsgiver = ArbeidsgiverFraSøknad("Test 1", "1111"),
        startSyketilfelle = LocalDate.now(),
        soktUtenlandsopphold = true,
        soknadsperioder = emptyList(),
        sendtNav = LocalDateTime.now(),
        tom = LocalDate.now(),
        fom = LocalDate.now(),
        status = "SENDT",
        andreInntektskilder = emptyList(),
        fravær = emptyList()
)

val soknadUtenVerdi = FaktagrunnlagResultat(
        originalSøknad = originalSoknad,
        faktagrunnlag = faktagrunnlagUtenVerdi
)

val enkleAvklarteVerdier = AvklarteVerdier(
        alder = Vurdering.Avklart(fastsattVerdi = 50, fastsattAv = "test", begrunnelse = "whatevs", grunnlag = Aldersgrunnlag(fodselsdato = LocalDate.now().minusYears(50))),
        medlemsskap = Vurdering.Avklart(fastsattVerdi = true, fastsattAv = "test", begrunnelse = "whatevs", grunnlag = Tpsfakta(LocalDate.now().minusYears(50), "NOR", "NOR", "BOSA", null)),
        arbeidsforhold = Vurdering.Avklart(fastsattVerdi = true, fastsattAv = "test", begrunnelse = "whatevs", grunnlag = emptyList()),
        opptjeningstid = Vurdering.Avklart(fastsattVerdi = 25L, fastsattAv = "test", begrunnelse = "whatevs", grunnlag = Opptjeningsgrunnlag(førsteSykdomsdag = LocalDate.now(), arbeidsforhold = emptyList())),
        sykepengegrunnlag = Vurdering.Avklart(
                fastsattVerdi = Sykepengegrunnlag(
                        sykepengegrunnlagNårTrygdenYter = Vurdering.Avklart(
                                fastsattVerdi = 1L,
                                fastsattAv = "test",
                                begrunnelse = "whatevs",
                                grunnlag = Beregningsperiode(
                                        inntekter = emptyMap(),
                                        begrunnelse = "whatevs")),
                        sykepengegrunnlagIArbeidsgiverperioden = Vurdering.Avklart(
                                fastsattVerdi = 1L,
                                fastsattAv = "test",
                                begrunnelse = "whatevs",
                                grunnlag = Beregningsperiode(
                                        inntekter = emptyMap(),
                                        begrunnelse = "whatevs"))),
                fastsattAv = "test",
                begrunnelse = "whatevs",
                grunnlag = Beregningsperiode(inntekter = emptyMap(), begrunnelse = "whatevs")),
        maksdato = Vurdering.Avklart(fastsattVerdi = LocalDate.now().plusDays(248), fastsattAv = "test", begrunnelse = "whatevs", grunnlag = Grunnlagsdata(førsteFraværsdag = LocalDate.now(), førsteSykepengedag = LocalDate.now(), tidligerePerioder = emptyList(), yrkesstatus = Yrkesstatus.ARBEIDSTAKER, personensAlder = 50)),
        sykepengehistorikk = emptyList())

val enkelSykepengeberegning: Sykepengeberegning =
        Sykepengeberegning(
                originalSøknad = originalSoknad,
                beregning = Beregningsresultat(dagsatser = listOf(Dagsats(dato = LocalDate.now(), sats = 1000L, skalUtbetales = true)), delresultater = emptyList()),
                avklarteVerdier = enkleAvklarteVerdier,
                faktagrunnlag = faktagrunnlagUtenVerdi,
                vilkårsprøving = Evaluering.ja("for reasons")
        )
