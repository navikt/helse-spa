package no.nav.helse

import no.nav.helse.streams.defaultObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BehandlingsfeilSerialiseringsfeil {
    @Test
    fun `bør kunne serialisere deserialiseringsfeil`() {
        val failJsonNode = defaultObjectMapper.readTree(failJson)
        val fail = Behandlingsfeil.ukjentDeserialiseringsfeil(failJsonNode, RuntimeException("yep. fail."))

        val serializedFail = serializeBehandlingsfeil(fail)
        Assertions.assertNotNull(serializedFail)
        println(serializedFail)
    }
}

val failJson = """{
    "inntekter": [
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2019-01-31",
                "fom": "2019-01-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {

                "fom": "2018-12-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-11-30",
                "fom": "2018-11-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-10-31",
                "fom": "2018-10-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-09-30",
                "fom": "2018-09-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-08-31",
                "fom": "2018-08-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-07-31",
                "fom": "2018-07-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-06-30",
                "fom": "2018-06-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-05-31",
                "fom": "2018-05-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-04-30",
                "fom": "2018-04-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-03-31",
                "fom": "2018-03-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-02-28",
                "fom": "2018-02-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2018-01-31",
                "fom": "2018-01-01"
            }
        },
        {
            "arbeidsgiver": {
                "orgnr": "97114455"
            },
            "beløp": 25000,
            "opptjeningsperiode": {
                "tom": "2017-12-31",
                "fom": "2017-12-01"
            }
        }
    ]
}"""