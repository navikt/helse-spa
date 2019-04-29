package no.nav.helse

import no.nav.helse.streams.defaultObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BehandlingsfeilSerialiseringsfeil {
    @Test
    fun `bør kunne serialisere deserialiseringsfeil`() {
        val failJsonNode = defaultObjectMapper.readTree(failJson)
        val fail = Behandlingsfeil.ukjentDeserialiseringsfeil("", failJsonNode, RuntimeException("yep. fail."))

        val serializedFail = serializeBehandlingsfeil(fail)
        Assertions.assertNotNull(serializedFail)
        println(serializedFail)

        val avklaringsfeil = defaultObjectMapper.readValue(uavklarteJson,Behandlingsfeil.Avklaringsfeil::class.java)
        println(avklaringsfeil.feilmelding)
    }
}

val uavklarteJson= """{
  "uavklarteFakta" : {
    "originalSøknad" : {
      "id": "12345",
      "aktorId" : "9903059340882",
      "type": "ARBEIDSTAKERE",
      "status" : "SENDT",
      "arbeidsgiver" : {
        "navn" : "AS MOCK",
        "orgnummer" : "995816598"
      },
      "soktUtenlandsopphold" : false,
      "fom" : "2019-01-11",
      "tom" : "2019-02-11",
      "startSyketilfelle" : "2019-01-11",
      "sendtNav" : "2019-03-11T13:07:41.672536",
      "soknadsperioder" : [ {
        "fom" : "2019-01-11",
        "tom" : "2019-02-11",
        "sykmeldingsgrad" : 100
      } ],
      "andreInntektskilder" : []
    },
    "faktagrunnlag" : {
      "tps" : {
        "fodselsdato" : "1981-07-12",
        "bostedland" : "SWE",
        "statsborgerskap": "NOR",
        "status": "BOSA"
      },
      "beregningsperiode" : [ {
        "virksomhet" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2018-10",
        "beløp" : 25000.0,
        "type": "Lønn",
        "ytelse": false
      }, {
        "virksomhet" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2018-11",
        "beløp" : 25000.0,
        "type": "Lønn",
        "ytelse": false
      }, {
        "virksomhet" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2018-12",
        "beløp" : 25000.0,
        "type": "Lønn",
        "ytelse": false
      } ],
      "sammenligningsperiode" : [ {
        "virksomhet" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2018-01",
        "beløp" : 25000.0,
        "type": "Lønn",
        "ytelse": false
      }, {
        "virksomhet" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2018-02",
        "beløp" : 25000.0,
        "type": "Lønn",
        "ytelse": false
      }, {
        "virksomhet" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2018-03",
        "beløp" : 25000.0,
        "type": "Lønn",
        "ytelse": false
      }, {
        "virksomhet" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2018-04",
        "beløp" : 25000.0,
        "type": "Lønn",
        "ytelse": false
      }, {
        "virksomhet" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2018-05",
        "beløp" : 25000.0,
        "type": "Lønn",
        "ytelse": false
      }, {
        "virksomhet" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2018-06",
        "beløp" : 25000.0,
        "type": "Lønn",
        "ytelse": false
      }, {
        "virksomhet" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2018-07",
        "beløp" : 25000.0,
        "type": "Lønn",
        "ytelse": false
      }, {
        "virksomhet" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2018-08",
        "beløp" : 25000.0,
        "type": "Lønn",
        "ytelse": false
      }, {
        "virksomhet" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2018-09",
        "beløp" : 25000.0,
        "type": "Lønn",
        "ytelse": false
      }, {
        "virksomhet" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2018-10",
        "beløp" : 25000.0,
        "type": "Lønn",
        "ytelse": false
      }, {
        "virksomhet" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
        "utbetalingsperiode": "2018-11",
        "beløp" : 25000.0,
        "type": "Lønn",
        "ytelse": false
      }, {
        "virksomhet" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
       "utbetalingsperiode": "2018-12",
        "beløp" : 25000.0,
        "type": "Lønn",
        "ytelse": false
      } ],
      "sykepengehistorikk" : [ ],
      "arbeidsforhold" : [ {
        "type": "Arbeidstaker",
        "arbeidsgiver" : {
          "identifikator" : "995816598",
          "type": "Organisasjon"
        },
        "startdato" : "2018-03-11",
        "sluttdato" : null
      } ]
    },
    "uavklarteVerdier" : {
      "medlemsskap" : {
        "type" : "Uavklart",
        "årsak" : "FALLER_UTENFOR_MVP",
        "begrunnelse" : "Søker må være bosatt i Norge",
        "grunnlag" : {
          "fodselsdato": "1980-01-01",
          "bostedland" : "SWE",
          "statsborgerskap": "NOR",
          "status": "BOSA"
        },
        "vurderingstidspunkt" : "2019-03-11T13:07:47.954472"
      },
      "alder" : {
        "type" : "Avklart",
        "fastsattVerdi" : 37,
        "begrunnelse" : "§ 8-51",
        "grunnlag" : {
          "fodselsdato" : "1981-07-12"
        },
        "fastsattAv" : "SPA",
        "vurderingstidspunkt" : "2019-03-11T13:07:47.955003"
      },
      "maksdato" : {
        "type" : "Avklart",
        "fastsattVerdi" : "2019-12-24",
        "begrunnelse" : "§ 8-12: ARBEIDSTAKER på 37 år gir maks 248 dager. 0 av disse er forbrukt",
        "grunnlag" : {
          "førsteFraværsdag" : "2019-01-11",
          "førsteSykepengedag" : "2019-01-11",
          "personensAlder" : 37,
          "yrkesstatus" : "ARBEIDSTAKER",
          "tidligerePerioder" : [ ]
        },
        "fastsattAv" : "SPA",
        "vurderingstidspunkt" : "2019-03-11T13:07:47.96384"
      },
      "sykepengehistorikk" : [ ],
      "arbeidsforhold" : {
        "type" : "Avklart",
        "fastsattVerdi" : true,
        "begrunnelse" : "Søker har en arbeidsgiver med orgnummer 995816598",
        "grunnlag" : [ {
          "type": "Arbeidstaker",
          "arbeidsgiver" : {
            "identifikator" : "995816598",
            "type": "Organisasjon"
          },
          "startdato" : "2018-03-11",
          "sluttdato" : null
        } ],
        "fastsattAv" : "SPA",
        "vurderingstidspunkt" : "2019-03-11T13:07:47.955464"
      },
      "opptjeningstid" : {
        "type" : "Avklart",
        "fastsattVerdi" : 306,
        "begrunnelse" : "Søker er i et aktivt arbeidsforhold",
        "grunnlag" : {
          "førsteSykdomsdag" : "2019-01-11",
          "arbeidsforhold" : [ {
            "type": "Arbeidstaker",
            "arbeidsgiver" : {
              "identifikator" : "995816598",
              "type": "Organisasjon"
            },
            "startdato" : "2018-03-11",
            "sluttdato" : null
          } ]
        },
        "fastsattAv" : "SPA",
        "vurderingstidspunkt" : "2019-03-11T13:07:47.957718"
      },
      "sykepengegrunnlag" : {
        "type" : "Avklart",
        "fastsattVerdi" : {
          "sykepengegrunnlagNårTrygdenYter" : {
            "type" : "Avklart",
            "fastsattVerdi" : 300000,
            "begrunnelse" : "§ 8-30 første ledd",
            "grunnlag" : {
              "inntekter" : [ {
                "virksomhet" : {
                  "identifikator" : "995816598",
                  "type": "Organisasjon"
                },
                "utbetalingsperiode": "2018-10",
                "beløp" : 25000.0,
                "type": "Lønn",
                "ytelse": false
              }, {
                "virksomhet" : {
                  "identifikator" : "995816598",
                  "type": "Organisasjon"
                },
                "utbetalingsperiode": "2018-11",
                "beløp" : 25000.0,
                "type": "Lønn",
                "ytelse": false
              }, {
                "virksomhet" : {
                  "identifikator" : "995816598",
                  "type": "Organisasjon"
                },
                "utbetalingsperiode": "2018-12",
                "beløp" : 25000.0,
                "type": "Lønn",
                "ytelse": false
              } ],
              "begrunnelse" : "§ 8-28 tredje ledd bokstav a) - De tre siste kalendermånedene før arbeidstakeren ble arbeidsufør (2019-01-11) legges til grunn."
            },
            "fastsattAv" : "spa",
            "vurderingstidspunkt" : "2019-03-11T13:07:47.959582"
          },
          "sykepengegrunnlagIArbeidsgiverperioden" : {
            "type" : "Avklart",
            "fastsattVerdi" : 25000,
            "begrunnelse" : "§ 8-28 andre ledd",
            "grunnlag" : {
              "inntekter" : [ {
                "virksomhet" : {
                  "identifikator" : "995816598",
                  "type": "Organisasjon"
                },
                "utbetalingsperiode": "2018-10",
                "beløp" : 25000.0,
                "type": "Lønn",
                "ytelse": false
              }, {
                "virksomhet" : {
                  "identifikator" : "995816598",
                  "type": "Organisasjon"
                },
                "utbetalingsperiode": "2018-11",
                "beløp" : 25000.0,
                "type": "Lønn",
                "ytelse": false
              }, {
                "virksomhet" : {
                  "identifikator" : "995816598",
                  "type": "Organisasjon"
                },
                "utbetalingsperiode": "2018-12",
                "beløp" : 25000.0,
                "type": "Lønn",
                "ytelse": false
              } ],
              "begrunnelse" : "§ 8-28 tredje ledd bokstav a) - De tre siste kalendermånedene før arbeidstakeren ble arbeidsufør (2019-01-11) legges til grunn."
            },
            "fastsattAv" : "spa",
            "vurderingstidspunkt" : "2019-03-11T13:07:47.959344"
          }
        },
        "begrunnelse" : "",
        "grunnlag" : {
          "inntekter" : [ {
            "virksomhet" : {
              "identifikator" : "995816598",
              "type": "Organisasjon"
            },
            "utbetalingsperiode": "2018-01",
            "beløp" : 25000.0,
            "type": "Lønn",
            "ytelse": false
          }, {
            "virksomhet" : {
              "identifikator" : "995816598",
              "type": "Organisasjon"
            },
            "utbetalingsperiode": "2018-02",
            "beløp" : 25000.0,
            "type": "Lønn",
            "ytelse": false
          }, {
            "virksomhet" : {
              "identifikator" : "995816598",
              "type": "Organisasjon"
            },
            "utbetalingsperiode": "2018-03",
            "beløp" : 25000.0,
            "type": "Lønn",
            "ytelse": false
          }, {
            "virksomhet" : {
              "identifikator" : "995816598",
              "type": "Organisasjon"
            },
            "utbetalingsperiode": "2018-04",
            "beløp" : 25000.0,
            "type": "Lønn",
            "ytelse": false
          }, {
            "virksomhet" : {
              "identifikator" : "995816598",
              "type": "Organisasjon"
            },
            "utbetalingsperiode": "2018-05",
            "beløp" : 25000.0,
            "type": "Lønn",
            "ytelse": false
          }, {
            "virksomhet" : {
              "identifikator" : "995816598",
              "type": "Organisasjon"
            },
            "utbetalingsperiode": "2018-06",
            "beløp" : 25000.0,
            "type": "Lønn",
            "ytelse": false
          }, {
            "virksomhet" : {
              "identifikator" : "995816598",
              "type": "Organisasjon"
            },
            "utbetalingsperiode": "2018-07",
            "beløp" : 25000.0,
            "type": "Lønn",
            "ytelse": false
          }, {
            "virksomhet" : {
              "identifikator" : "995816598",
              "type": "Organisasjon"
            },
            "utbetalingsperiode": "2018-08",
            "beløp" : 25000.0,
            "type": "Lønn",
            "ytelse": false
          }, {
            "virksomhet" : {
              "identifikator" : "995816598",
              "type": "Organisasjon"
            },
            "utbetalingsperiode": "2018-09",
            "beløp" : 25000.0,
            "type": "Lønn",
            "ytelse": false
          }, {
            "virksomhet" : {
              "identifikator" : "995816598",
              "type": "Organisasjon"
            },
            "utbetalingsperiode": "2018-10",
            "beløp" : 25000.0,
            "type": "Lønn",
            "ytelse": false
          }, {
            "virksomhet" : {
              "identifikator" : "995816598",
              "type": "Organisasjon"
            },
            "utbetalingsperiode": "2018-11",
            "beløp" : 25000.0,
            "type": "Lønn",
            "ytelse": false
          }, {
            "virksomhet" : {
              "identifikator" : "995816598",
              "type": "Organisasjon"
            },
            "utbetalingsperiode": "2018-12",
            "beløp" : 25000.0,
            "type": "Lønn",
            "ytelse": false
          } ],
          "begrunnelse" : "§ 8-30 andre ledd - rapportert inntekt (se § 8-29) til a-ordningen etter reglene i a-opplysningsloven de siste tolv kalendermånedene før arbeidsuførheten inntraff (2019-01-11) legges til grunn."
        },
        "fastsattAv" : "SPA",
        "vurderingstidspunkt" : "2019-03-11T13:07:47.959915"
      }
    }
  },
  "feilmelding" : "Kunne ikke fastsette alle fakta."
}
"""
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
