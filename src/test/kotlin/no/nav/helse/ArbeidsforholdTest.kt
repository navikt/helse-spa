package no.nav.helse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.serde.defaultObjectMapper
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
        assertEquals("1111111111", arbeidsforhold.organisasjoner.get(0).organisasjonsnummer)
    }

    @Test
    fun `vurder arbeidsforhold med en arbeidsgiver`() {
        val arbeidsforholdFakta = ArbeidsforholdFakta(listOf(ArbeidsgiverFakta("1111", "Test 1")), LocalDate.now(), LocalDate.now())
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, inntekt = inntektFaktaUtenVerdi, arbeidsforhold = arbeidsforholdFakta,
                sykepengeliste = emptyList())
        val vurdering = vurderArbeidsforhold(BeriketSykepengesøknad(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Avklart) assertThat(vurdering.fastsattVerdi).isTrue() else fail("Feil vurdering!")

    }

    @Test
    fun `vurder arbeidsforhold med feil arbeidsgiver`() {
        val arbeidsforholdFakta = ArbeidsforholdFakta(listOf(ArbeidsgiverFakta("2222", "Test 2")),LocalDate.now(), LocalDate.now())
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, inntekt = inntektFaktaUtenVerdi, arbeidsforhold = arbeidsforholdFakta,
                sykepengeliste = emptyList())
        val vurdering = vurderArbeidsforhold(BeriketSykepengesøknad(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Avklart) assertThat(vurdering.fastsattVerdi).isFalse() else fail("Feil vurdering!")

    }

    @Test
    fun `vurder arbeidsforhold med flere arbeidsgiverer`() {
        val arbeidsforholdFakta = ArbeidsforholdFakta(listOf(ArbeidsgiverFakta("1111", "Test 1"),
                ArbeidsgiverFakta("2222", "Test 2")), LocalDate.now(), LocalDate.now())
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, inntekt = inntektFaktaUtenVerdi, arbeidsforhold = arbeidsforholdFakta,
                sykepengeliste = emptyList())
        val vurdering = vurderArbeidsforhold(BeriketSykepengesøknad(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Uavklart) assertThat(vurdering.arsak==Vurdering.Uavklart.Arsak.SKJONN).isTrue() else fail("Feil vurdering!")

    }

}