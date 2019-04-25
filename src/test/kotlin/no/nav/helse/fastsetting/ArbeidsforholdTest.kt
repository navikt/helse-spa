package no.nav.helse.fastsetting

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.behandling.Faktagrunnlag
import no.nav.helse.behandling.FaktagrunnlagResultat
import no.nav.helse.domain.Arbeidsforhold
import no.nav.helse.domain.ArbeidsforholdWrapper
import no.nav.helse.domain.Arbeidsgiver
import no.nav.helse.originalSoknad
import no.nav.helse.streams.defaultObjectMapper
import no.nav.helse.tpsFaktaUtenVerdi
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.LocalDate
class ArbeidsforholdTest {

    private val log = LoggerFactory.getLogger(ArbeidsforholdTest::class.java)

    @Test
    fun hentArbeidsforhold() {
        val arbeidsforhold : List<Arbeidsforhold> = defaultObjectMapper.readValue(ArbeidsforholdTest::class.java.
                classLoader.getResourceAsStream("arbeidsforhold.json"))
        assertEquals("1111111111", arbeidsforhold[0].arbeidsgiver.identifikator)
    }

    @Test
    fun `vurder arbeidsforhold med en arbeidsgiver`() {
        val arbeidsforhold = listOf(Arbeidsforhold("Arbeidstaker", Arbeidsgiver("1111", "Organisasjon"), LocalDate.now(), null))
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, beregningsperiode = emptyList(), sammenligningsperiode = emptyList(), arbeidsforhold = arbeidsforhold,
                sykepengehistorikk = emptyList())
        val vurdering = vurderArbeidsforhold(FaktagrunnlagResultat(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Avklart) assertThat(vurdering.fastsattVerdi).isTrue() else fail("Feil vurdering!")

    }

    @Test
    fun `vurder arbeidsforhold med feil arbeidsgiver`() {
        val arbeidsforhold = listOf(Arbeidsforhold("Arbeidstaker", Arbeidsgiver("2222", "Organisasjon"), LocalDate.now(), null))
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, beregningsperiode = emptyList(), sammenligningsperiode = emptyList(), arbeidsforhold = arbeidsforhold,
                sykepengehistorikk = emptyList())
        val vurdering = vurderArbeidsforhold(FaktagrunnlagResultat(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Uavklart) assertThat(vurdering.årsak== Vurdering.Uavklart.Årsak.HAR_IKKE_DATA).isTrue() else fail("Feil vurdering!")

    }

    @Test
    fun `vurder arbeidsforhold med flere arbeidsgiverer`() {
        val arbeidsforhold = listOf(Arbeidsforhold("Arbeidstaker", Arbeidsgiver("1111", "Organisasjon"), LocalDate.now(), null),
                Arbeidsforhold("Arbeidstaker", Arbeidsgiver("2222", "Organisasjon"), LocalDate.now(), null))
        val faktagrunnlag = Faktagrunnlag(tps = tpsFaktaUtenVerdi, beregningsperiode = emptyList(), sammenligningsperiode = emptyList(), arbeidsforhold = arbeidsforhold,
                sykepengehistorikk = emptyList())
        val vurdering = vurderArbeidsforhold(FaktagrunnlagResultat(originalSoknad, faktagrunnlag))
        if (vurdering is Vurdering.Avklart) assertThat(vurdering.fastsattVerdi).isTrue() else fail("Feil vurdering!")

    }

    @Test
    fun `skal parse json fra sparkel`() {
        val result = defaultObjectMapper.readValue<ArbeidsforholdWrapper>(jsonFromSparkel)
        assertNotNull(result)
    }

}

val jsonFromSparkel = """
{
  "arbeidsforhold": [
    {
      "arbeidsforhold": {
        "arbeidsgiver": {
          "identifikator": "995816598",
          "type": "Organisasjon"
        },
        "startdato": "2009-01-15",
        "type": "Arbeidstaker"
      }
    }
  ]
}
""".trimIndent()
