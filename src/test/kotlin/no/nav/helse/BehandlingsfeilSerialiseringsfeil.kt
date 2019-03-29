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
      } ]
    },
    "faktagrunnlag" : {
      "tps" : {
        "fodselsdato" : "1981-07-12",
        "bostedland" : "SWE"
      },
      "beregningsperiode" : [ {
        "arbeidsgiver" : {
          "orgnr" : "995816598"
        },
        "opptjeningsperiode" : {
          "fom" : "2018-10-01",
          "tom" : "2018-10-31",
          "antattPeriode" : false
        },
        "beløp" : 25000.0
      }, {
        "arbeidsgiver" : {
          "orgnr" : "995816598"
        },
        "opptjeningsperiode" : {
          "fom" : "2018-11-01",
          "tom" : "2018-11-30",
          "antattPeriode" : false
        },
        "beløp" : 25000.0
      }, {
        "arbeidsgiver" : {
          "orgnr" : "995816598"
        },
        "opptjeningsperiode" : {
          "fom" : "2018-12-01",
          "tom" : "2018-12-31",
          "antattPeriode" : false
        },
        "beløp" : 25000.0
      } ],
      "sammenligningsperiode" : [ {
        "arbeidsgiver" : {
          "orgnr" : "995816598"
        },
        "opptjeningsperiode" : {
          "fom" : "2018-01-01",
          "tom" : "2018-01-31",
          "antattPeriode" : false
        },
        "beløp" : 25000.0
      }, {
        "arbeidsgiver" : {
          "orgnr" : "995816598"
        },
        "opptjeningsperiode" : {
          "fom" : "2018-02-01",
          "tom" : "2018-02-28",
          "antattPeriode" : false
        },
        "beløp" : 25000.0
      }, {
        "arbeidsgiver" : {
          "orgnr" : "995816598"
        },
        "opptjeningsperiode" : {
          "fom" : "2018-03-01",
          "tom" : "2018-03-31",
          "antattPeriode" : false
        },
        "beløp" : 25000.0
      }, {
        "arbeidsgiver" : {
          "orgnr" : "995816598"
        },
        "opptjeningsperiode" : {
          "fom" : "2018-04-01",
          "tom" : "2018-04-30",
          "antattPeriode" : false
        },
        "beløp" : 25000.0
      }, {
        "arbeidsgiver" : {
          "orgnr" : "995816598"
        },
        "opptjeningsperiode" : {
          "fom" : "2018-05-01",
          "tom" : "2018-05-31",
          "antattPeriode" : false
        },
        "beløp" : 25000.0
      }, {
        "arbeidsgiver" : {
          "orgnr" : "995816598"
        },
        "opptjeningsperiode" : {
          "fom" : "2018-06-01",
          "tom" : "2018-06-30",
          "antattPeriode" : false
        },
        "beløp" : 25000.0
      }, {
        "arbeidsgiver" : {
          "orgnr" : "995816598"
        },
        "opptjeningsperiode" : {
          "fom" : "2018-07-01",
          "tom" : "2018-07-31",
          "antattPeriode" : false
        },
        "beløp" : 25000.0
      }, {
        "arbeidsgiver" : {
          "orgnr" : "995816598"
        },
        "opptjeningsperiode" : {
          "fom" : "2018-08-01",
          "tom" : "2018-08-31",
          "antattPeriode" : false
        },
        "beløp" : 25000.0
      }, {
        "arbeidsgiver" : {
          "orgnr" : "995816598"
        },
        "opptjeningsperiode" : {
          "fom" : "2018-09-01",
          "tom" : "2018-09-30",
          "antattPeriode" : false
        },
        "beløp" : 25000.0
      }, {
        "arbeidsgiver" : {
          "orgnr" : "995816598"
        },
        "opptjeningsperiode" : {
          "fom" : "2018-10-01",
          "tom" : "2018-10-31",
          "antattPeriode" : false
        },
        "beløp" : 25000.0
      }, {
        "arbeidsgiver" : {
          "orgnr" : "995816598"
        },
        "opptjeningsperiode" : {
          "fom" : "2018-11-01",
          "tom" : "2018-11-30",
          "antattPeriode" : false
        },
        "beløp" : 25000.0
      }, {
        "arbeidsgiver" : {
          "orgnr" : "995816598"
        },
        "opptjeningsperiode" : {
          "fom" : "2018-12-01",
          "tom" : "2018-12-31",
          "antattPeriode" : false
        },
        "beløp" : 25000.0
      } ],
      "sykepengeliste" : [ ],
      "arbeidsforhold" : [ {
        "arbeidsgiver" : {
          "navn" : "AS, [",
          "orgnummer" : "995816598"
        },
        "startdato" : "2018-03-11",
        "sluttdato" : null
      } ]
    },
    "uavklarteVerdier" : {
      "medlemsskap" : {
        "type" : "Uavklart",
        "årsak" : "KREVER_SKJØNNSMESSIG_VURDERING",
        "begrunnelse" : "(Søker er ikke bostatt i Norge. ELLER Vi har ikke nok informasjon til å kunne gi et entydig svar.)",
        "grunnlag" : {
          "bostedsland" : "SWE"
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
      "sykepengeliste" : [ ],
      "arbeidsforhold" : {
        "type" : "Avklart",
        "fastsattVerdi" : true,
        "begrunnelse" : "Søker har en arbeidsgiver med orgnummer 995816598",
        "grunnlag" : [ {
          "arbeidsgiver" : {
            "navn" : "AS, [",
            "orgnummer" : "995816598"
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
            "arbeidsgiver" : {
              "navn" : "AS, [",
              "orgnummer" : "995816598"
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
                "arbeidsgiver" : {
                  "orgnr" : "995816598"
                },
                "opptjeningsperiode" : {
                  "fom" : "2018-10-01",
                  "tom" : "2018-10-31",
                  "antattPeriode" : false
                },
                "beløp" : 25000.0
              }, {
                "arbeidsgiver" : {
                  "orgnr" : "995816598"
                },
                "opptjeningsperiode" : {
                  "fom" : "2018-11-01",
                  "tom" : "2018-11-30",
                  "antattPeriode" : false
                },
                "beløp" : 25000.0
              }, {
                "arbeidsgiver" : {
                  "orgnr" : "995816598"
                },
                "opptjeningsperiode" : {
                  "fom" : "2018-12-01",
                  "tom" : "2018-12-31",
                  "antattPeriode" : false
                },
                "beløp" : 25000.0
              } ],
              "begrunnelse" : "§ 8-28 tredje ledd bokstav a) – De tre siste kalendermånedene før arbeidstakeren ble arbeidsufør (2019-01-11) legges til grunn."
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
                "arbeidsgiver" : {
                  "orgnr" : "995816598"
                },
                "opptjeningsperiode" : {
                  "fom" : "2018-10-01",
                  "tom" : "2018-10-31",
                  "antattPeriode" : false
                },
                "beløp" : 25000.0
              }, {
                "arbeidsgiver" : {
                  "orgnr" : "995816598"
                },
                "opptjeningsperiode" : {
                  "fom" : "2018-11-01",
                  "tom" : "2018-11-30",
                  "antattPeriode" : false
                },
                "beløp" : 25000.0
              }, {
                "arbeidsgiver" : {
                  "orgnr" : "995816598"
                },
                "opptjeningsperiode" : {
                  "fom" : "2018-12-01",
                  "tom" : "2018-12-31",
                  "antattPeriode" : false
                },
                "beløp" : 25000.0
              } ],
              "begrunnelse" : "§ 8-28 tredje ledd bokstav a) – De tre siste kalendermånedene før arbeidstakeren ble arbeidsufør (2019-01-11) legges til grunn."
            },
            "fastsattAv" : "spa",
            "vurderingstidspunkt" : "2019-03-11T13:07:47.959344"
          }
        },
        "begrunnelse" : "",
        "grunnlag" : {
          "inntekter" : [ {
            "arbeidsgiver" : {
              "orgnr" : "995816598"
            },
            "opptjeningsperiode" : {
              "fom" : "2018-01-01",
              "tom" : "2018-01-31",
              "antattPeriode" : false
            },
            "beløp" : 25000.0
          }, {
            "arbeidsgiver" : {
              "orgnr" : "995816598"
            },
            "opptjeningsperiode" : {
              "fom" : "2018-02-01",
              "tom" : "2018-02-28",
              "antattPeriode" : false
            },
            "beløp" : 25000.0
          }, {
            "arbeidsgiver" : {
              "orgnr" : "995816598"
            },
            "opptjeningsperiode" : {
              "fom" : "2018-03-01",
              "tom" : "2018-03-31",
              "antattPeriode" : false
            },
            "beløp" : 25000.0
          }, {
            "arbeidsgiver" : {
              "orgnr" : "995816598"
            },
            "opptjeningsperiode" : {
              "fom" : "2018-04-01",
              "tom" : "2018-04-30",
              "antattPeriode" : false
            },
            "beløp" : 25000.0
          }, {
            "arbeidsgiver" : {
              "orgnr" : "995816598"
            },
            "opptjeningsperiode" : {
              "fom" : "2018-05-01",
              "tom" : "2018-05-31",
              "antattPeriode" : false
            },
            "beløp" : 25000.0
          }, {
            "arbeidsgiver" : {
              "orgnr" : "995816598"
            },
            "opptjeningsperiode" : {
              "fom" : "2018-06-01",
              "tom" : "2018-06-30",
              "antattPeriode" : false
            },
            "beløp" : 25000.0
          }, {
            "arbeidsgiver" : {
              "orgnr" : "995816598"
            },
            "opptjeningsperiode" : {
              "fom" : "2018-07-01",
              "tom" : "2018-07-31",
              "antattPeriode" : false
            },
            "beløp" : 25000.0
          }, {
            "arbeidsgiver" : {
              "orgnr" : "995816598"
            },
            "opptjeningsperiode" : {
              "fom" : "2018-08-01",
              "tom" : "2018-08-31",
              "antattPeriode" : false
            },
            "beløp" : 25000.0
          }, {
            "arbeidsgiver" : {
              "orgnr" : "995816598"
            },
            "opptjeningsperiode" : {
              "fom" : "2018-09-01",
              "tom" : "2018-09-30",
              "antattPeriode" : false
            },
            "beløp" : 25000.0
          }, {
            "arbeidsgiver" : {
              "orgnr" : "995816598"
            },
            "opptjeningsperiode" : {
              "fom" : "2018-10-01",
              "tom" : "2018-10-31",
              "antattPeriode" : false
            },
            "beløp" : 25000.0
          }, {
            "arbeidsgiver" : {
              "orgnr" : "995816598"
            },
            "opptjeningsperiode" : {
              "fom" : "2018-11-01",
              "tom" : "2018-11-30",
              "antattPeriode" : false
            },
            "beløp" : 25000.0
          }, {
            "arbeidsgiver" : {
              "orgnr" : "995816598"
            },
            "opptjeningsperiode" : {
              "fom" : "2018-12-01",
              "tom" : "2018-12-31",
              "antattPeriode" : false
            },
            "beløp" : 25000.0
          } ],
          "begrunnelse" : "§ 8-30 andre ledd – rapportert inntekt (se § 8-29) til a-ordningen etter reglene i a-opplysningsloven de siste tolv kalendermånedene før arbeidsuførheten inntraff (2019-01-11) legges til grunn."
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
