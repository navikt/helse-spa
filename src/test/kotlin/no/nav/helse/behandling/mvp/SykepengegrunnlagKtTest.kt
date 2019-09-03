package no.nav.helse.behandling.mvp

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.EndToEndTest
import no.nav.helse.behandling.Sakskompleks
import no.nav.helse.oppslag.Inntekt
import no.nav.helse.oppslag.Inntektsarbeidsgiver
import no.nav.helse.readResource
import no.nav.helse.sykepenger.beregning.Sykepengegrunnlag
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal
import java.time.YearMonth

internal class SykepengegrunnlagKtTest {

    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private val sakskompleksJson = objectMapper.readTree("/sakskompleks/sakskompleks.json".readResource())
    private val sakskompleks = Sakskompleks(sakskompleksJson)

    val beregningsgrunnlagStart = sakskompleks.startSyketilfelle.minusMonths(3)
    private val beregningsgrunnlag: List<Inntekt> = List(3, init = { index ->
        Inntekt(
            virksomhet = Inntektsarbeidsgiver("97114455", "Organisasjon"),
            beløp = BigDecimal.valueOf(25000),
            utbetalingsperiode = YearMonth.from(beregningsgrunnlagStart.plusMonths(index.toLong())),
            type = "Lønn",
            ytelse = false,
            kode = null
        )
    })

    val sammenligningsgrunnlagStart = sakskompleks.startSyketilfelle.minusMonths(12)
    private val sammenligningsgrunnlag: List<Inntekt> = List(12, init = { index ->
        Inntekt(
            virksomhet = Inntektsarbeidsgiver("97114455", "Organisasjon"),
            beløp = BigDecimal.valueOf(25000),
            utbetalingsperiode = YearMonth.from(sammenligningsgrunnlagStart.plusMonths(index.toLong())),
            type = "Lønn",
            ytelse = false,
            kode = null
        )
    })

    @Test
    fun `happy case der sakskompleks ikke bryter noen kriterier`() {
        val feil = vurderMVPKriterierForSykepengegrunnlaget(sakskompleks, beregningsgrunnlag, sammenligningsgrunnlag)
        assertEquals(0, feil.size)
    }

    @Test
    fun `lønn i inntektsmelding er over 5 prosent høyere enn beregningsgrunnlag gir feil`() {
        val sakskompleksJson = objectMapper.readTree("/sakskompleks/sakskompleks.json".readResource()) as ObjectNode
        val inntektsmelding = sakskompleksJson["inntektsmeldinger"][0] as ObjectNode
        inntektsmelding.replace("inntekt", JsonNodeFactory.instance.numberNode(30200))
        inntektsmelding.replace("refusjon", JsonNodeFactory.instance.numberNode(30200))
        (sakskompleksJson["inntektsmeldinger"] as ArrayNode).removeAll().add(inntektsmelding)
        val sakskompleks = Sakskompleks(sakskompleksJson)

        val feil = vurderMVPKriterierForSykepengegrunnlaget(sakskompleks, beregningsgrunnlag, sammenligningsgrunnlag)
        assertEquals(1, feil.size)
        assertEquals("Avvik over 5%. Beregnet månedsinntekt: 25000, oppgitt i inntektsmeldingen: 30200", feil[0].årsak)
    }

    @Test
    fun `lønn i inntektsmelding er mindre enn 5 prosent lavere enn beregningsgrunnlag gir feil`() {
        val sakskompleksJson = objectMapper.readTree("/sakskompleks/sakskompleks.json".readResource()) as ObjectNode
        val inntektsmelding = sakskompleksJson["inntektsmeldinger"][0] as ObjectNode
        inntektsmelding.replace("inntekt", JsonNodeFactory.instance.numberNode(20000))
        inntektsmelding.replace("refusjon", JsonNodeFactory.instance.numberNode(20000))
        (sakskompleksJson["inntektsmeldinger"] as ArrayNode).removeAll().add(inntektsmelding)
        val sakskompleks = Sakskompleks(sakskompleksJson)

        val feil = vurderMVPKriterierForSykepengegrunnlaget(sakskompleks, beregningsgrunnlag, sammenligningsgrunnlag)
        assertEquals(1, feil.size)
        assertEquals("Avvik over 5%. Beregnet månedsinntekt: 25000, oppgitt i inntektsmeldingen: 20000", feil[0].årsak)
    }
}
