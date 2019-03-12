package no.nav.helse.sensu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DataPointTest {

    @Test
    fun `should handle complex tags and fields`() {
        val dataPoint = DataPoint("myEvent", mapOf(
                "field=1" to "value \"with\" quotes",
                "field,2" to 1,
                "field 3" to false
        ), mapOf(
                "tag=1" to "one two",
                "tag,2" to "one \"two\"",
                "tag 3" to "one,two=three"
        ))
        val expected = "myEvent,tag\\=1=one\\ two,tag\\,2=one\\ \"two\",tag\\ 3=one\\,two\\=three field\\=1=\"value \\\"with\\\" quotes\",field\\,2=1i,field\\ 3=false ${dataPoint.timeInMilliseconds*1000000}"

        assertEquals(expected, dataPoint.toLineProtocol())
    }

    @Test
    fun `should handle field with string value`() {
        val dataPoint = DataPoint("myEvent", mapOf("field" to "val"))
        val expected = "myEvent field=\"val\" ${dataPoint.timeInMilliseconds*1000000}"

        assertEquals(expected, dataPoint.toLineProtocol())
    }

    @Test
    fun `should handle field with string value with quote`() {
        val dataPoint = DataPoint("myEvent", mapOf("field" to "val \"foo\""))
        val expected = "myEvent field=\"val \\\"foo\\\"\" ${dataPoint.timeInMilliseconds*1000000}"

        assertEquals(expected, dataPoint.toLineProtocol())
    }

    @Test
    fun `should handle field with comma in key`() {
        val dataPoint = DataPoint("myEvent", mapOf("key1,2" to "val2"))
        val expected = "myEvent key1\\,2=\"val2\" ${dataPoint.timeInMilliseconds*1000000}"

        assertEquals(expected, dataPoint.toLineProtocol())
    }

    @Test
    fun `should handle field with equal sign in key`() {
        val dataPoint = DataPoint("myEvent", mapOf("key1=2" to "val2"))
        val expected = "myEvent key1\\=2=\"val2\" ${dataPoint.timeInMilliseconds*1000000}"

        assertEquals(expected, dataPoint.toLineProtocol())
    }

    @Test
    fun `should handle tags with comma in key`() {
        val dataPoint = DataPoint("myEvent", mapOf("field" to "val1"), mapOf("key1,2" to "val2"))
        val expected = "myEvent,key1\\,2=val2 field=\"val1\" ${dataPoint.timeInMilliseconds*1000000}"

        assertEquals(expected, dataPoint.toLineProtocol())
    }

    @Test
    fun `should handle tags with equal sign in key`() {
        val dataPoint = DataPoint("myEvent", mapOf("field" to "val1"), mapOf("key1=2" to "val2"))
        val expected = "myEvent,key1\\=2=val2 field=\"val1\" ${dataPoint.timeInMilliseconds*1000000}"

        assertEquals(expected, dataPoint.toLineProtocol())
    }

    @Test
    fun `should handle tags with comma in value`() {
        val dataPoint = DataPoint("myEvent", mapOf("field" to "val1"), mapOf("key1" to "val2,val3"))
        val expected = "myEvent,key1=val2\\,val3 field=\"val1\" ${dataPoint.timeInMilliseconds*1000000}"

        assertEquals(expected, dataPoint.toLineProtocol())
    }

    @Test
    fun `should handle field with int value`() {
        val dataPoint = DataPoint("myEvent", mapOf("field" to 1))
        val expected = "myEvent field=1i ${dataPoint.timeInMilliseconds*1000000}"

        assertEquals(expected, dataPoint.toLineProtocol())
    }

    @Test
    fun `should handle field with double value`() {
        val dataPoint = DataPoint("myEvent", mapOf("field" to 1.0))
        val expected = "myEvent field=1.0 ${dataPoint.timeInMilliseconds*1000000}"

        assertEquals(expected, dataPoint.toLineProtocol())
    }

    @Test
    fun `should handle field with boolean false value`() {
        val dataPoint = DataPoint("myEvent", mapOf("field" to false))
        val expected = "myEvent field=false ${dataPoint.timeInMilliseconds*1000000}"

        assertEquals(expected, dataPoint.toLineProtocol())
    }

    @Test
    fun `should handle field with boolean true value`() {
        val dataPoint = DataPoint("myEvent", mapOf("field" to true))
        val expected = "myEvent field=true ${dataPoint.timeInMilliseconds*1000000}"

        assertEquals(expected, dataPoint.toLineProtocol())
    }

    @Test
    fun `should send event with tags`() {
        val dataPoint = DataPoint("myEvent", mapOf("field" to "val1"), mapOf("tag" to "val2"))
        val expected = "myEvent,tag=val2 field=\"val1\" ${dataPoint.timeInMilliseconds*1000000}"

        assertEquals(expected, dataPoint.toLineProtocol())
    }
}
