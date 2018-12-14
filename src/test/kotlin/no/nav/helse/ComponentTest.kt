package no.nav.helse

import io.prometheus.client.CollectorRegistry
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.streams.JsonDeserializer
import no.nav.helse.streams.JsonSerializer
import no.nav.helse.streams.Topics
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

class ComponentTest {
    companion object {
        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        val embeddedEnvironment = KafkaEnvironment(
                users = listOf(JAASCredential(username, password)),
                autoStart = false,
                withSchemaRegistry = false,
                withSecurity = true,
                topics = listOf(Topics.SYKEPENGEBEHANDLING.name, Topics.VEDTAK_SYKEPENGER.name)
        )

        private val env = Environment(username = username,
                password = password,
                bootstrapServersUrl = embeddedEnvironment.brokersURL)

        @BeforeAll
        @JvmStatic
        fun setup() {
            CollectorRegistry.defaultRegistry.clear()
            embeddedEnvironment.start()
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            embeddedEnvironment.tearDown()
        }
    }

    private fun producerProperties(): Properties {
        return Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer::class.java)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";")
        }
    }

    private fun consumerProperties(): Properties {
        return Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer::class.java)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";")
            put(ConsumerConfig.GROUP_ID_CONFIG, "spinne-test-verification")
        }
    }

    @Test
    fun `embedded kafka cluster is up and running`() {
        assertEquals(embeddedEnvironment.serverPark.status, KafkaEnvironment.ServerParkStatus.Started)
    }

    @Test
    fun `give an application the spa treatment`() {
        val spa = SaksbehandlingStream(env)
        spa.start()

        val soknad = JSONObject(Soknad("1",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "sendt"))

        val producer = KafkaProducer<String, JSONObject>(producerProperties())
        producer.send(ProducerRecord(Topics.SYKEPENGEBEHANDLING.name, null, soknad))
        producer.flush()

        val consumer = KafkaConsumer<String, JSONObject>(consumerProperties())
        consumer.subscribe(listOf(Topics.VEDTAK_SYKEPENGER.name))
        val records = consumer.poll(Duration.ofSeconds(10))

        assertEquals(1, records.count())

        val vedtak = records.first().value()
        assertThat(vedtak["belop"]).isEqualTo(1000)

        spa.stop()
    }

}