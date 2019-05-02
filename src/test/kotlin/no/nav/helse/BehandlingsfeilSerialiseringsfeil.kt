package no.nav.helse

import no.nav.helse.streams.defaultObjectMapper
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class BehandlingsfeilSerialiseringsfeil {
    @Test
    fun `bør kunne serialisere deserialiseringsfeil`() {
        val failJsonNode = defaultObjectMapper.readTree(failJson)
        val fail = Behandlingsfeil.ukjentDeserialiseringsfeil("", failJsonNode, RuntimeException("yep. fail."))

        val serializedFail = serializeBehandlingsfeil(fail)
        assertNotNull(serializedFail)
    }
}

val failJson = """{
    "inntekter": [
        {
            "virksomhet": {
                "identifikator": "97114455",
                "type": "Organisasjon"
            },
            "beløp": 25000,
            "utbetalingsperiode": "2019-01",
            "type": "Lønn",
            "ytelse": false
        },
        {
            "virksomhet": {
                "identifikator": "97114455",
                "type": "Organisasjon"
            },
            "beløp": 25000,
            "utbetalingsperiode": "2018-12",
            "type": "Lønn",
            "ytelse": false
        },
        {
            "virksomhet": {
                "identifikator": "97114455",
                "type": "Organisasjon"
            },
            "beløp": 25000,
            "utbetalingsperiode": "2018-11",
            "type": "Lønn",
            "ytelse": false
        },
        {
            "virksomhet": {
                "identifikator": "97114455",
                "type": "Organisasjon"
            },
            "beløp": 25000,
            "utbetalingsperiode": "2018-10",
            "type": "Lønn",
            "ytelse": false
        },
        {
            "virksomhet": {
                "identifikator": "97114455",
                "type": "Organisasjon"
            },
            "beløp": 25000,
            "utbetalingsperiode": "2018-09",
            "type": "Lønn",
            "ytelse": false
        },
        {
            "virksomhet": {
                "identifikator": "97114455",
                "type": "Organisasjon"
            },
            "beløp": 25000,
            "utbetalingsperiode": "2018-08",
            "type": "Lønn",
            "ytelse": false
        },
        {
            "virksomhet": {
                "identifikator": "97114455",
                "type": "Organisasjon"
            },
            "beløp": 25000,
            "utbetalingsperiode": "2018-07",
            "type": "Lønn",
            "ytelse": false
        },
        {
            "virksomhet": {
                "identifikator": "97114455",
                "type": "Organisasjon"
            },
            "beløp": 25000,
            "utbetalingsperiode": "2018-06",
            "type": "Lønn",
            "ytelse": false
        },
        {
            "virksomhet": {
                "identifikator": "97114455",
                "type": "Organisasjon"
            },
            "beløp": 25000,
            "utbetalingsperiode": "2018-05",
            "type": "Lønn",
            "ytelse": false
        },
        {
            "virksomhet": {
                "identifikator": "97114455",
                "type": "Organisasjon"
            },
            "beløp": 25000,
            "utbetalingsperiode": "2018-04",
            "type": "Lønn",
            "ytelse": false
        },
        {
            "virksomhet": {
                "identifikator": "97114455",
                "type": "Organisasjon"
            },
            "beløp": 25000,
            "utbetalingsperiode": "2018-03",
            "type": "Lønn",
            "ytelse": false
        },
        {
            "virksomhet": {
                "identifikator": "97114455",
                "type": "Organisasjon"
            },
            "beløp": 25000,
            "utbetalingsperiode": "2018-02",
            "type": "Lønn",
            "ytelse": false
        },
        {
            "virksomhet": {
                "identifikator": "97114455",
                "type": "Organisasjon"
            },
            "beløp": 25000,
            "utbetalingsperiode": "2018-01",
            "type": "Lønn",
            "ytelse": false
        },
        {
            "virksomhet": {
                "identifikator": "97114455",
                "type": "Organisasjon"
            },
            "beløp": 25000,
            "utbetalingsperiode": "2017-12",
            "type": "Lønn",
            "ytelse": false
        }
    ]
}"""
