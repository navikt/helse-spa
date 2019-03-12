package no.nav.helse.sensu

import io.mockk.ConstantAnswer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class InfluxDBTest {

    @Test
    fun `puts default tags on given datapoint`() {
        val sensuClient = mockk<SensuClient>()

        val defaultTags = mapOf("tag1" to "val2")
        val dataPoint = DataPoint("myEvent", mapOf("field" to "val"))
        val expected = dataPoint.copy(tags = dataPoint.tags + defaultTags)

        every {
            sensuClient.sendEvent(match { event ->
                event.name == "check-app"
                        && event.output == expected.toLineProtocol()
            })
        } answers(ConstantAnswer(Unit))

        InfluxMetricReporter(sensuClient, "check-app", defaultTags).sendDataPoint(dataPoint)

        verify(exactly = 1) {
            sensuClient.sendEvent(any())
        }
    }

    @Test
    fun `puts default tags on created data point`() {
        val sensuClient = mockk<SensuClient>()

        val defaultTags = mapOf("tag1" to "val2")

        every {
            sensuClient.sendEvent(any())
        } answers(ConstantAnswer(Unit))

        val actual = InfluxMetricReporter(sensuClient, "check-app", defaultTags).sendDataPoint("myEvent", mapOf("field" to "val"))

        val expected = DataPoint("myEvent", mapOf("field" to "val"), defaultTags, time = actual.time)

        verify(exactly = 1) {
            sensuClient.sendEvent(match { event ->
                event.name == "check-app"
                        && event.output == expected.toLineProtocol()
            })
        }
    }
}
