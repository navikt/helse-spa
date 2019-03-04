package no.nav.helse

import com.fasterxml.jackson.databind.*
import com.github.tomakehurst.wiremock.*
import com.github.tomakehurst.wiremock.client.WireMock.any
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.*
import no.nav.common.*
import no.nav.helse.behandling.*
import no.nav.helse.domain.*
import no.nav.helse.serde.*
import no.nav.helse.streams.*
import org.apache.kafka.clients.*
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.clients.producer.*
import org.apache.kafka.common.config.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.*
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

        restStsStub()
        personStub(aktørId)
        inntektStub(aktørId)
        arbeidsforholdStub(aktørId)

        produserSøknad(aktørId)

        val actual = ventPåVedtak()!!

        assertNotNull(actual)
        assertNotNull(actual.get("avklarteVerdier"))
        assertEquals(actual.get("avklarteVerdier").get("medlemsskap").get("fastsattVerdi").booleanValue(), true)
        assertEquals(actual.get("avklarteVerdier").get("medlemsskap").get("fastsattAv").textValue(), "SPA")
    }

    private fun produserSøknad(aktørId: String) {
        val søknad = Sykepengesøknad(
                aktorId = aktørId,
                status = "SENDT",
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

    private fun produceOneMessage(message: Sykepengesøknad) {
        val producer = KafkaProducer<String, Sykepengesøknad>(producerProperties())
        producer.send(ProducerRecord(sykepengesoknadTopic.name, message))
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
                .willReturn(okJson("""
{
    "arbeidsforhold": [
        {
            "arbeidsgiver": {
                "navn": "EQUINOR ASA, AVD STATOIL SOKKELVIRKSOMHET",
                "orgnummer": "97114455"
            },
            "startdato": "2017-01-01"
        }
    ]
}
""")))
    }
}
