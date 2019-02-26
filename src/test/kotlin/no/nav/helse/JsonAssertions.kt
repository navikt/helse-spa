package no.nav.helse

import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions

fun assertJsonEquals(expected: JSONObject, actual: JSONObject, skipKeys: List<String> = emptyList()) {
    Assertions.assertEquals(expected.length(), actual.length(), "$actual does not match $expected")

    expected.keys().forEach {
        Assertions.assertTrue(actual.has(it), "$it is not present in $actual")

        if (it in skipKeys) {
            return
        }

        val expectedValue = expected.get(it)
        when(expectedValue) {
            is JSONObject -> assertJsonEquals(expectedValue, actual.get(it) as JSONObject, skipKeys)
            is JSONArray -> assertJsonEquals(expectedValue, actual.get(it) as JSONArray, skipKeys)
            else -> Assertions.assertEquals(expectedValue, actual.get(it), "${actual.get(it)} does not match $expectedValue")
        }
    }
}

fun assertJsonEquals(expected: JSONArray, actual: JSONArray, skipKeys: List<String>) {
    Assertions.assertEquals(expected.length(), actual.length())

    expected.forEachIndexed { index, it ->
        when(it) {
            is JSONObject -> assertJsonEquals(it, actual.get(index) as JSONObject, skipKeys)
            is JSONArray -> assertJsonEquals(it, actual.get(index) as JSONArray, skipKeys)
            else -> Assertions.assertEquals(it, actual.get(index), "${actual.get(index)} does not match $it")
        }
    }
}
