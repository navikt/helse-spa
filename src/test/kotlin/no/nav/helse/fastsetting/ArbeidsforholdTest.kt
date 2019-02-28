package no.nav.helse.fastsetting

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.Arbeidsforhold
import no.nav.helse.ArbeidsforholdFakta
import no.nav.helse.ArbeidsgiverFakta
import no.nav.helse.FaktagrunnlagResultat
import no.nav.helse.Faktagrunnlag
import no.nav.helse.originalSoknad
import no.nav.helse.serde.defaultObjectMapper
import no.nav.helse.tpsFaktaUtenVerdi
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.LocalDate

class ArbeidsforholdTest {

    private val log = LoggerFactory.getLogger(ArbeidsforholdTest::class.java)

    @Test
    fun hentArbeidsforhold() {
        val arbeidsforhold : Arbeidsforhold = defaultObjectMapper.readValue(ArbeidsforholdTest::class.java.
                classLoader.getResourceAsStream("arbeidsforhold.json"))
        assertEquals("1111111111", arbeidsforhold.arbeidsforhold.get(0).arbeidsgiver.organisasjonsnummer)
    }

    @Test
    fun `vurder arbeidsforhold med en arbeidsgiver`() {
        val arbeidsforholdFakta = ArbeidsforholdFakta(listOf(ArbeidsgiverFakta("1111", "Test 1", LocalDate.now(), null)))
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, beregningsperiode = emptyList(), sammenligningsperiode = emptyList(), arbeidsforhold = arbeidsforholdFakta,
                sykepengeliste = emptyList())
        val vurdering = vurderArbeidsforhold(FaktagrunnlagResultat(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Avklart) assertThat(vurdering.fastsattVerdi).isTrue() else fail("Feil vurdering!")

    }

    @Test
    fun `vurder arbeidsforhold med feil arbeidsgiver`() {
        val arbeidsforholdFakta = ArbeidsforholdFakta(listOf(ArbeidsgiverFakta("2222", "Test 2", LocalDate.now(), null)))
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, beregningsperiode = emptyList(), sammenligningsperiode = emptyList(), arbeidsforhold = arbeidsforholdFakta,
                sykepengeliste = emptyList())
        val vurdering = vurderArbeidsforhold(FaktagrunnlagResultat(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Avklart) assertThat(vurdering.fastsattVerdi).isFalse() else fail("Feil vurdering!")

    }

    @Test
    fun `vurder arbeidsforhold med flere arbeidsgiverer`() {
        val arbeidsforholdFakta = ArbeidsforholdFakta(listOf(ArbeidsgiverFakta("1111", "Test 1", LocalDate.now(), null),
                ArbeidsgiverFakta("2222", "Test 2", LocalDate.now(), null)))
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, beregningsperiode = emptyList(), sammenligningsperiode = emptyList(), arbeidsforhold = arbeidsforholdFakta,
                sykepengeliste = emptyList())
        val vurdering = vurderArbeidsforhold(FaktagrunnlagResultat(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Uavklart) assertThat(vurdering.årsak== Vurdering.Uavklart.Årsak.KREVER_SKJØNNSMESSIG_VURDERING).isTrue() else fail("Feil vurdering!")

    }

}
