package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.serde.JacksonNodeDeserializer
import no.nav.helse.serde.JacksonSerializer
import no.nav.helse.streams.JsonDeserializer
import no.nav.helse.streams.Topics
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.util.*

class EndToEndTest {

    companion object {
        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        val embeddedEnvironment = KafkaEnvironment(
                users = listOf(JAASCredential(username, password)),
                autoStart = false,
                withSchemaRegistry = false,
                withSecurity = true,
                topics = listOf(sykepengesoknadTopic.name, Topics.VEDTAK_SYKEPENGER.name)
        )

        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        private lateinit var app: SaksbehandlingStream

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
            embeddedEnvironment.start()

            startSpa()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            stopSpa()

            server.stop()
            embeddedEnvironment.tearDown()
        }

        private fun startSpa() {
            val env = Environment(
                    username = username,
                    password = password,
                    kafkaUsername = username,
                    kafkaPassword = password,
                    bootstrapServersUrl = embeddedEnvironment.brokersURL,
                    sparkelBaseUrl = server.baseUrl(),
                    stsRestUrl = server.baseUrl()
            )

            app = SaksbehandlingStream(env)
            app.start()
        }

        private fun stopSpa() {
            app.stop()
        }
    }

    @BeforeEach
    fun configure() {
        configureFor(server.port())
    }

    @Test
    fun `behandle en søknad`() {
        val aktørId = "11987654321"

        println("Kafka: ${embeddedEnvironment.brokersURL}")
        println("Zookeeper: ${embeddedEnvironment.serverPark.zookeeper.host}:${embeddedEnvironment.serverPark.zookeeper.port}")

        restStsStub()
        personStub(aktørId)
        inntektStub(aktørId)
        arbeidsforholdStub(aktørId)

        produserSøknad(aktørId)

        val expected = forventetVedtak()
        val actual = ventPåVedtak()!!

        println(actual)

        assertNotNull(actual)
        assertEquals(actual.get("medlemsskap").get("fastsattVerdi").booleanValue(), true)
        assertEquals(actual.get("medlemsskap").get("fastsattAv").textValue(), "SPA")
    }

    private fun forventetVedtak() = """
{
  "maksdato": {
    "grunnlag": {
      "førsteFraværsdag": "2019-01-01",
      "førsteSykepengedag": "2019-01-01",
      "personensAlder": 48,
      "yrkesstatus": "ARBEIDSTAKER",
      "tidligerePerioder": []
    },
    "begrunnelse": "§ 8-12: ARBEIDSTAKER på 48 år gir maks 248 dager. 0 av disse er forbrukt",
    "fastsattVerdi": "2019-12-12",
    "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
    "fastsattAv": "SPA"
  },
  "medlemsskap": {
    "grunnlag": {
      "bostedsland": "NOR"
    },
    "begrunnelse": "(Søker er bosatt i Norge. ELLER Vi har ikke nok informasjon til å kunne gi et entydig svar.)",
    "fastsattVerdi": true,
    "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
    "fastsattAv": "SPA"
  },
  "sykepengegrunnlag": {
    "begrunnelse": "",
    "grunnlag": {
      "begrunnelse": "§ 8-30 andre ledd – rapportert inntekt (se § 8-29) til a-ordningen etter reglene i a-opplysningsloven de siste tolv kalendermånedene før arbeidsuførheten inntraff (2019-01-01) legges til grunn.",
      "inntekter": [
        {
          "arbeidsgiver": {
            "orgnr": "97114455"
          },
          "beløp": 25000,
          "opptjeningsperiode": {
            "tom": "2018-12-31",
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
        }
      ]
    },
    "fastsattVerdi": {
      "sykepengegrunnlagNårTrygdenYter": {
        "begrunnelse": "§ 8-30 første ledd",
        "grunnlag": {
          "begrunnelse": "§ 8-28 tredje ledd bokstav a) \u2013 De tre siste kalendermånedene før arbeidstakeren ble arbeidsufør (2019-01-01) legges til grunn.",
          "inntekter": [
            {
              "arbeidsgiver": {
                "orgnr": "97114455"
              },
              "beløp": 25000,
              "opptjeningsperiode": {
                "tom": "2018-12-31",
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
            }
          ]
        },
        "fastsattVerdi": 300000,
        "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
        "fastsattAv": "spa"
      },
      "sykepengegrunnlagIArbeidsgiverperioden": {
        "begrunnelse": "§ 8-28 andre ledd",
        "grunnlag": {
          "begrunnelse": "§ 8-28 tredje ledd bokstav a) \u2013 De tre siste kalendermånedene før arbeidstakeren ble arbeidsufør (2019-01-01) legges til grunn.",
          "inntekter": [
            {
              "arbeidsgiver": {
                "orgnr": "97114455"
              },
              "beløp": 25000,
              "opptjeningsperiode": {
                "tom": "2018-12-31",
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
            }
          ]
        },
        "fastsattVerdi": 25000,
        "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
        "fastsattAv": "spa"
      }
    },
    "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
    "fastsattAv": "SPA"
  },
  "arbeidsforhold": {
    "grunnlag": {
      "arbeidsgivere": [
        {
          "navn": "EQUINOR ASA, AVD STATOIL SOKKELVIRKSOMHET",
          "organisasjonsnummer": "97114455",
          "startdato":"2017-01-01"
        }
      ]
    },
    "begrunnelse": "Søker har en arbeidsgiver med orgnummer 97114455",
    "fastsattVerdi": true,
    "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
    "fastsattAv": "SPA"
  },
  "opptjeningstid": {
    "grunnlag": {
      "førsteSykdomsdag": "2019-01-01",
      "arbeidsforhold": [
        {
          "navn": "EQUINOR ASA, AVD STATOIL SOKKELVIRKSOMHET",
          "organisasjonsnummer": "97114455",
          "startdato":"2017-01-01"
        }
      ]
    },
    "begrunnelse": "Søker er i et aktivt arbeidsforhold",
    "fastsattVerdi": 730,
    "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
    "fastsattAv": "SPA"
  },
  "sykepengeliste": [],
    "aktorId": "11987654321",
    "tom": "2019-01-31",
    "arbeidsgiver": {
      "orgnummer": "97114455",
      "navn": "NAV"
    },
    "soktUtenlandsopphold": false,
    "fom": "2019-01-01",
    "soknadsperioder": [
      {
        "tom": "2019-01-31",
        "fom": "2019-01-01",
        "sykmeldingsgrad": 100
      }
    ],
    "harVurdertInntekt": false,
    "startSyketilfelle": "2019-01-01",
    "sendtNav": "2019-01-17T00:00",

  "alder": {
    "grunnlag": {
      "fodselsdato": "1970-09-01"
    },
    "begrunnelse": "§ 8-51",
    "fastsattVerdi": 48,
    "vurderingstidspunkt": "BLIR_IKKE_MATCHET",
    "fastsattAv": "SPA"
  }
}
""".trimIndent()

    private fun produserSøknad(aktørId: String) {
        val søknad = Sykepengesoknad(
                aktorId = aktørId,
                arbeidsgiver = Arbeidsgiver("NAV", "97114455"),
                soktUtenlandsopphold = false,
                fom = LocalDate.parse("2019-01-01"),
                tom = LocalDate.parse("2019-01-31"),
                startSyketilfelle = LocalDate.parse("2019-01-01"),
                sendtNav = LocalDate.parse("2019-01-17").atStartOfDay(),
                soknadsperioder = listOf(
                        Soknadsperiode(
                                fom = LocalDate.parse("2019-01-01"),
                                tom = LocalDate.parse("2019-01-31"),
                                sykmeldingsgrad = 100
                        )
                ),
                harVurdertInntekt = false
        )
        produceOneMessage(søknad)
    }

    private fun ventPåVedtak(): JsonNode? {
        val resultConsumer = KafkaConsumer<String, JsonNode>(consumerProperties())
        resultConsumer.subscribe(listOf(sykepengevedtakTopic.name))

        val end = System.currentTimeMillis() + 60 * 1000

        while (System.currentTimeMillis() < end) {
            resultConsumer.seekToBeginning(resultConsumer.assignment())
            val records = resultConsumer.poll(Duration.ofSeconds(1))

            if (!records.isEmpty) {
                assertEquals(1, records.count())

                return records.records(sykepengevedtakTopic.name).map {
                    it.value()
                }.first()
            }
        }

        return null
    }

    private fun consumerProperties(): Properties {
        return Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)

            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonNodeDeserializer::class.java)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";")
            put(ConsumerConfig.GROUP_ID_CONFIG, "spa-e2e-verification")
        }
    }

    private fun produceOneMessage(message: Sykepengesoknad) {
        val producer = KafkaProducer<String, Sykepengesoknad>(producerProperties())
        producer.send(ProducerRecord(sykepengesoknadTopic.name, null, message))
        producer.flush()
    }

    private fun producerProperties(): Properties {
        return Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonSerializer::class.java)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";")
        }
    }

    private fun restStsStub() {
        stubFor(any(urlPathEqualTo("/rest/v1/sts/token"))
                .willReturn(okJson("""{
    "access_token": "test token",
    "token_type": "Bearer",
    "expires_in": 3600
}""")))
    }

    private fun personStub(aktørId: String) {
        stubFor(any(urlPathEqualTo("/api/person/$aktørId"))
                .willReturn(okJson("""{
    "id": {
        "aktor": "1078277661159"
    },
    "fdato": "1970-09-01",
    "fornavn": "MAX",
    "etternavn": "SMEKKER",
    "kjønn": "MANN",
    "bostedsland": "NOR"
}""")))
    }

    private fun inntektStub(aktørId: String) {
        stubFor(any(urlPathEqualTo("/api/inntekt/$aktørId/beregningsgrunnlag"))
                .willReturn(okJson("""{
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
                "tom": "2018-12-31",
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
}""")))

        stubFor(any(urlPathEqualTo("/api/inntekt/$aktørId/sammenligningsgrunnlag"))
                .willReturn(okJson("""{
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
                "tom": "2018-12-31",
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
}""")))
    }

    private fun arbeidsforholdStub(aktørId: String) {
        stubFor(any(urlPathEqualTo("/api/arbeidsforhold/$aktørId"))
                .willReturn(okJson("""{
    "arbeidsforhold": [
        {
            "arbeidsgiver": {
                "navn": "EQUINOR ASA, AVD STATOIL SOKKELVIRKSOMHET",
                "organisasjonsnummer": "97114455"
            },
            "startdato": "2017-01-01"
        }
    ]
}""")))
    }
}
