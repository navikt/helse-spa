package no.nav.helse.sensu

data class DataPoint(val name: String, val fields: Map<String, Any>, val tags: Map<String, String> = emptyMap(), val time: Long = System.nanoTime()) {

    init {
        if (fields.isEmpty()) {
            throw IllegalStateException("a data point must have at least one field")
        }
    }

    fun toLineProtocol() =
            String.format("%s%s%s %d", escapeMeasurement(name), if (tags.isNotEmpty()) "," + tags.toCSV() else "", if (fields.isNotEmpty()) " " + transformFields(fields) else "", time)

    private fun transformFields(fields: Map<String, Any>) =
            fields.map { entry ->
                "${escapeTagKeysAndValues(entry.key)}=" + when (entry.value) {
                    is String -> "\"${escapeFieldValue(entry.value as String)}\""
                    is Boolean -> entry.value
                    is Int -> "${entry.value}i"
                    else -> entry.value
                }
            }.joinToString(separator = ",")

    private fun Map<String, String>.toCSV() =
            map { entry ->
                "${escapeTagKeysAndValues(entry.key)}=${escapeTagKeysAndValues(entry.value)}"
            }.joinToString(separator = ",")

    private fun escapeTagKeysAndValues(str: String) =
            str.replace("=", "\\=")
                    .replace(",", "\\,")
                    .replace(" ", "\\ ")

    private fun escapeMeasurement(str: String) =
            str.replace(",", "\\,")
                    .replace(" ", "\\ ")

    private fun escapeFieldValue(str: String) =
            str.replace("\"", "\\\"")
}

class InfluxMetricReporter(private val sensuClient: SensuClient, private val defaultTags: Map<String, String> = emptyMap()) {

    fun sendDataPoint(measurement: String, fields: Map<String, Any> = emptyMap(), tags: Map<String, String> = emptyMap()) =
            DataPoint(name = measurement, fields = fields, tags = addDefaultTags(tags)).also {
                sendDataPoint(it)
            }

    fun sendDataPoint(dataPoint: DataPoint) =
            dataPoint.copy(tags = addDefaultTags(dataPoint.tags)).let {
                createSensuEvent(it.name, it.toLineProtocol()).also { event ->
                    sensuClient.sendEvent(event)
                }
            }

    private fun addDefaultTags(tags: Map<String, String>) = tags + defaultTags

    companion object {
        private fun createSensuEvent(eventName: String, output: String) = SensuEvent(eventName, "metric", "events_nano", output)
    }
}
