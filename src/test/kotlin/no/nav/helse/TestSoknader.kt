package no.nav.helse

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.behandling.*
import no.nav.helse.behandling.søknad.Sykepengesøknad
import no.nav.helse.dto.*
import no.nav.helse.fastsetting.Aldersgrunnlag
import no.nav.helse.fastsetting.Opptjeningsgrunnlag
import no.nav.helse.fastsetting.Sykepengegrunnlag
import no.nav.helse.fastsetting.Vurdering
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidInntektYtelseDTO
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsforholdDTO
import no.nav.helse.oppslag.arbeidinntektytelse.dto.YtelserDTO
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

val arbeidsforholdUtenVerdi = emptyList<ArbeidsforholdDTO>()

val faktagrunnlagUtenVerdi = Faktagrunnlag(
        tps = tpsFaktaUtenVerdi,
        beregningsperiode = emptyList(),
        sammenligningsperiode = emptyList(),
        sykepengehistorikk = emptyList(),
        arbeidInntektYtelse = ArbeidInntektYtelseDTO(
                arbeidsforhold = arbeidsforholdUtenVerdi,
                inntekter = emptyList(),
                ytelser = emptyList()
        ),
        ytelser = YtelserDTO(
                infotrygd = emptyList(),
                arena = emptyList()
        )
)

val originalSoknad = Sykepengesøknad(SykepengesøknadV2DTO(
        id = "1",
        aktorId = "1",
        type = SoknadstypeDTO.ARBEIDSTAKERE,
        arbeidsgiver = ArbeidsgiverDTO("Test 1", "1111"),
        startSyketilfelle = LocalDate.now(),
        soktUtenlandsopphold = true,
        soknadsperioder = emptyList(),
        sendtNav = LocalDateTime.now(),
        tom = LocalDate.now(),
        fom = LocalDate.now(),
        status = SoknadsstatusDTO.SENDT,
        andreInntektskilder = emptyList(),
        fravar = emptyList()
).asJsonNode())

val soknadUtenVerdi = FaktagrunnlagResultat(
        sakskompleks = Sakskompleks(originalSoknad),
        faktagrunnlag = faktagrunnlagUtenVerdi
)

fun Sakskompleks(enesteSøknad: Sykepengesøknad): Sakskompleks {
        val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        val sakskompleksJson = objectMapper.readTree("/sakskompleks/tomtSakskompleks.json".readResource()) as ObjectNode
        (sakskompleksJson["søknader"] as ArrayNode).addAll(listOf(enesteSøknad.jsonNode))
        sakskompleksJson.replace("syketilfelleStartDato", enesteSøknad.jsonNode["startSyketilfelle"])
        sakskompleksJson.replace("orgnummer", enesteSøknad.jsonNode["arbeidsgiver"]["orgnummer"])
        sakskompleksJson.replace("syketilfelleStartdato", enesteSøknad.jsonNode["startSyketilfelle"])
        sakskompleksJson.replace("syketilfelleSluttdato", enesteSøknad.jsonNode["tom"])
        return Sakskompleks(sakskompleksJson)
}

val enkleAvklarteVerdier = AvklarteVerdier(
        alder = Vurdering.Avklart(fastsattVerdi = 50, fastsattAv = "test", begrunnelse = "whatevs", grunnlag = Aldersgrunnlag(fodselsdato = LocalDate.now().minusYears(50))),
        medlemsskap = Vurdering.Avklart(fastsattVerdi = true, fastsattAv = "test", begrunnelse = "whatevs", grunnlag = Tpsfakta(LocalDate.now().minusYears(50), "NOR", "NOR", "BOSA", null)),
        arbeidsforhold = Vurdering.Avklart(fastsattVerdi = ArbeidsforholdDTO("", no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsgiverDTO("", ""), LocalDate.now(), null), grunnlag = arbeidsforholdUtenVerdi, begrunnelse = "derfor", fastsattAv = "test"),
        opptjeningstid = Vurdering.Avklart(fastsattVerdi = 25L, fastsattAv = "test", begrunnelse = "whatevs", grunnlag = Opptjeningsgrunnlag(førsteSykdomsdag = LocalDate.now(), arbeidsforhold = Vurdering.Avklart(fastsattVerdi = ArbeidsforholdDTO("", no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsgiverDTO("", ""), LocalDate.now(), null), grunnlag = arbeidsforholdUtenVerdi, begrunnelse = "derfor", fastsattAv = "test"))),
        sykepengegrunnlag = Vurdering.Avklart(
                fastsattVerdi = Sykepengegrunnlag(
                        sykepengegrunnlagNårTrygdenYter = Vurdering.Avklart(
                                fastsattVerdi = 1L,
                                fastsattAv = "test",
                                begrunnelse = "whatevs",
                                grunnlag = emptyList()),
                        sykepengegrunnlagIArbeidsgiverperioden = Vurdering.Avklart(
                                fastsattVerdi = 1L,
                                fastsattAv = "test",
                                begrunnelse = "whatevs",
                                grunnlag = emptyList())),
                fastsattAv = "test",
                begrunnelse = "whatevs",
                grunnlag = emptyList()),
        maksdato = Vurdering.Avklart(fastsattVerdi = LocalDate.now().plusDays(248), fastsattAv = "test", begrunnelse = "whatevs", grunnlag = Grunnlagsdata(førsteFraværsdag = LocalDate.now(), førsteSykepengedag = LocalDate.now(), tidligerePerioder = emptyList(), yrkesstatus = Yrkesstatus.ARBEIDSTAKER, personensAlder = 50)),
        sykepengehistorikk = emptyList())

val enkelSykepengeberegning: Sykepengeberegning =
        Sykepengeberegning(
                sakskompleks = Sakskompleks(originalSoknad),
                beregning = Beregningsresultat(dagsatser = listOf(Dagsats(dato = LocalDate.now(), sats = 1000L, skalUtbetales = true)), delresultater = emptyList()),
                beregningFraInntektsmelding = Beregningsresultat(dagsatser = listOf(Dagsats(dato = LocalDate.now(), sats = 1000L, skalUtbetales = true)), delresultater = emptyList()),
                avklarteVerdier = enkleAvklarteVerdier,
                faktagrunnlag = faktagrunnlagUtenVerdi,
                vilkårsprøving = Evaluering.ja("for reasons")
        )
