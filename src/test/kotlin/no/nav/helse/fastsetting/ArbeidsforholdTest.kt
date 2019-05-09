package no.nav.helse.fastsetting

import no.nav.helse.behandling.Faktagrunnlag
import no.nav.helse.behandling.FaktagrunnlagResultat
import no.nav.helse.oppslag.arbeidinntektytelse.dto.*
import no.nav.helse.originalSoknad
import no.nav.helse.tpsFaktaUtenVerdi
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class ArbeidsforholdTest {

    @Test
    fun `vurder arbeidsforhold med en arbeidsgiver`() {
        val arbeidsforhold = listOf(ArbeidsforholdDTO("Arbeidstaker", ArbeidsgiverDTO("1111", "Organisasjon"), LocalDate.now(), null))
        val arbeidInntektYtelse = ArbeidInntektYtelseDTO(
                arbeidsforhold = arbeidsforhold,
                inntekter = listOf(
                        InntektMedArbeidsforholdDTO(
                                inntekt = InntektDTO(
                                        virksomhet = VirksomhetDTO("1111", "Organisasjon"),
                                        utbetalingsperiode = YearMonth.now(),
                                        beløp = BigDecimal.ONE
                                ),
                                muligeArbeidsforhold = arbeidsforhold
                        )
                ),
                ytelser = emptyList()
        )
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, beregningsperiode = emptyList(), sammenligningsperiode = emptyList(), arbeidInntektYtelse = arbeidInntektYtelse,
                sykepengehistorikk = emptyList())
        val vurdering = vurderArbeidsforhold(FaktagrunnlagResultat(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Avklart) assertThat(vurdering.fastsattVerdi).isEqualTo(arbeidsforhold[0]) else fail("Feil vurdering!")

    }

    @Test
    fun `vurder arbeidsforhold med feil arbeidsgiver`() {
        val arbeidsforhold = listOf(ArbeidsforholdDTO("Arbeidstaker", ArbeidsgiverDTO("2222", "Organisasjon"), LocalDate.now(), null))
        val arbeidInntektYtelse = ArbeidInntektYtelseDTO(
                arbeidsforhold = arbeidsforhold,
                inntekter = listOf(
                        InntektMedArbeidsforholdDTO(
                                inntekt = InntektDTO(
                                        virksomhet = VirksomhetDTO("2222", "Organisasjon"),
                                        utbetalingsperiode = YearMonth.now(),
                                        beløp = BigDecimal.ONE
                                ),
                                muligeArbeidsforhold = arbeidsforhold
                        )
                ),
                ytelser = emptyList()
        )
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, beregningsperiode = emptyList(), sammenligningsperiode = emptyList(), arbeidInntektYtelse = arbeidInntektYtelse,
                sykepengehistorikk = emptyList())
        val vurdering = vurderArbeidsforhold(FaktagrunnlagResultat(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Uavklart) assertThat(vurdering.årsak== Vurdering.Uavklart.Årsak.HAR_IKKE_DATA).isTrue() else fail("Feil vurdering!")

    }

    @Test
    fun `vurder arbeidsforhold med flere arbeidsgiverer`() {
        val arbeidsforhold = listOf(
                ArbeidsforholdDTO("Arbeidstaker", ArbeidsgiverDTO("1111", "Organisasjon"), LocalDate.now(), null),
                ArbeidsforholdDTO("Arbeidstaker", ArbeidsgiverDTO("2222", "Organisasjon"), LocalDate.now(), null)
        )
        val arbeidInntektYtelse = ArbeidInntektYtelseDTO(
                arbeidsforhold = arbeidsforhold,
                inntekter = listOf(
                        InntektMedArbeidsforholdDTO(
                                inntekt = InntektDTO(
                                        virksomhet = VirksomhetDTO("1111", "Organisasjon"),
                                        utbetalingsperiode = YearMonth.now(),
                                        beløp = BigDecimal.ONE
                                ),
                                muligeArbeidsforhold = listOf(arbeidsforhold[0])
                        )
                ),
                ytelser = emptyList()
        )
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, beregningsperiode = emptyList(), sammenligningsperiode = emptyList(), arbeidInntektYtelse = arbeidInntektYtelse,
                sykepengehistorikk = emptyList())
        val vurdering = vurderArbeidsforhold(FaktagrunnlagResultat(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Avklart) assertThat(vurdering.fastsattVerdi).isEqualTo(arbeidsforhold[0]) else fail("Feil vurdering!")

    }
}
