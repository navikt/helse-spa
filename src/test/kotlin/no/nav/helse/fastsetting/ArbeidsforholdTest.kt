package no.nav.helse.fastsetting

import com.fasterxml.jackson.module.kotlin.*
import no.nav.helse.*
import no.nav.helse.behandling.*
import no.nav.helse.domain.*
import no.nav.helse.serde.*
import org.assertj.core.api.Assertions.*
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.slf4j.*
import java.time.*

class ArbeidsforholdTest {

    private val log = LoggerFactory.getLogger(ArbeidsforholdTest::class.java)

    @Test
    fun hentArbeidsforhold() {
        val arbeidsforhold : List<Arbeidsforhold> = defaultObjectMapper.readValue(ArbeidsforholdTest::class.java.
                classLoader.getResourceAsStream("arbeidsforhold.json"))
        assertEquals("1111111111", arbeidsforhold[0].arbeidsgiver.orgnummer)
    }

    @Test
    fun `vurder arbeidsforhold med en arbeidsgiver`() {
        val arbeidsforhold = listOf(Arbeidsforhold(Arbeidsgiver("Test 1", "1111"), LocalDate.now(), null))
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, beregningsperiode = emptyList(), sammenligningsperiode = emptyList(), arbeidsforhold = arbeidsforhold,
                sykepengeliste = emptyList())
        val vurdering = vurderArbeidsforhold(FaktagrunnlagResultat(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Avklart) assertThat(vurdering.fastsattVerdi).isTrue() else fail("Feil vurdering!")

    }

    @Test
    fun `vurder arbeidsforhold med feil arbeidsgiver`() {
        val arbeidsforhold = listOf(Arbeidsforhold(Arbeidsgiver("Test 2", "2222"), LocalDate.now(), null))
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, beregningsperiode = emptyList(), sammenligningsperiode = emptyList(), arbeidsforhold = arbeidsforhold,
                sykepengeliste = emptyList())
        val vurdering = vurderArbeidsforhold(FaktagrunnlagResultat(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Avklart) assertThat(vurdering.fastsattVerdi).isFalse() else fail("Feil vurdering!")

    }

    @Test
    fun `vurder arbeidsforhold med flere arbeidsgiverer`() {
        val arbeidsforhold = listOf(Arbeidsforhold(Arbeidsgiver("Test 1", "1111"), LocalDate.now(), null),
                Arbeidsforhold(Arbeidsgiver("Test 2", "2222"), LocalDate.now(), null))
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, beregningsperiode = emptyList(), sammenligningsperiode = emptyList(), arbeidsforhold = arbeidsforhold,
                sykepengeliste = emptyList())
        val vurdering = vurderArbeidsforhold(FaktagrunnlagResultat(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Uavklart) assertThat(vurdering.årsak== Vurdering.Uavklart.Årsak.KREVER_SKJØNNSMESSIG_VURDERING).isTrue() else fail("Feil vurdering!")

    }

}
