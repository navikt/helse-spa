package no.nav.helse.fastsetting

import no.nav.helse.behandling.Faktagrunnlag
import no.nav.helse.behandling.FaktagrunnlagResultat
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidInntektYtelseDTO
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsforholdDTO
import no.nav.helse.oppslag.arbeidinntektytelse.dto.ArbeidsgiverDTO
import no.nav.helse.originalSoknad
import no.nav.helse.tpsFaktaUtenVerdi
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.time.LocalDate
class ArbeidsforholdTest {

    @Test
    fun `vurder arbeidsforhold med en arbeidsgiver`() {
        val arbeidInntektYtelse = ArbeidInntektYtelseDTO(
                arbeidsforhold = listOf(ArbeidsforholdDTO("Arbeidstaker", ArbeidsgiverDTO("1111", "Organisasjon"), LocalDate.now(), null)),
                inntekter = emptyList()
        )
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, beregningsperiode = emptyList(), sammenligningsperiode = emptyList(), arbeidInntektYtelse = arbeidInntektYtelse,
                sykepengehistorikk = emptyList())
        val vurdering = vurderArbeidsforhold(FaktagrunnlagResultat(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Avklart) assertThat(vurdering.fastsattVerdi).isTrue() else fail("Feil vurdering!")

    }

    @Test
    fun `vurder arbeidsforhold med feil arbeidsgiver`() {
        val arbeidInntektYtelse = ArbeidInntektYtelseDTO(
                arbeidsforhold = listOf(ArbeidsforholdDTO("Arbeidstaker", ArbeidsgiverDTO("2222", "Organisasjon"), LocalDate.now(), null)),
                inntekter = emptyList()
        )
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, beregningsperiode = emptyList(), sammenligningsperiode = emptyList(), arbeidInntektYtelse = arbeidInntektYtelse,
                sykepengehistorikk = emptyList())
        val vurdering = vurderArbeidsforhold(FaktagrunnlagResultat(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Uavklart) assertThat(vurdering.årsak== Vurdering.Uavklart.Årsak.HAR_IKKE_DATA).isTrue() else fail("Feil vurdering!")

    }

    @Test
    fun `vurder arbeidsforhold med flere arbeidsgiverer`() {
        val arbeidInntektYtelse = ArbeidInntektYtelseDTO(
                arbeidsforhold = listOf(
                        ArbeidsforholdDTO("Arbeidstaker", ArbeidsgiverDTO("1111", "Organisasjon"), LocalDate.now(), null),
                        ArbeidsforholdDTO("Arbeidstaker", ArbeidsgiverDTO("2222", "Organisasjon"), LocalDate.now(), null)
                ),
                inntekter = emptyList()
        )
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, beregningsperiode = emptyList(), sammenligningsperiode = emptyList(), arbeidInntektYtelse = arbeidInntektYtelse,
                sykepengehistorikk = emptyList())
        val vurdering = vurderArbeidsforhold(FaktagrunnlagResultat(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Avklart) assertThat(vurdering.fastsattVerdi).isTrue() else fail("Feil vurdering!")

    }
}
