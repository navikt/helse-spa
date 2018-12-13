package no.nav.helse

import org.apache.kafka.streams.kstream.ValueMapper
import org.json.JSONObject
import java.lang.Exception
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class JSONToSoknadMapper : ValueMapper<JSONObject, Soknad> {
    override fun apply(value: JSONObject): Soknad {
         return Soknad(value.getString("id"),
                 value.getOptString("aktorId"),
                 value.getOptString("sykmeldingId"),
                 value.getOptString("soknadstype"),
                 value.getOptLocalDate("innsendtDato", "yyyy-MM-dd"),
                 value.getOptLocalDate("tom", "yyyy-MM-dd"),
                 value.getOptLocalDate("fom", "yyyy-MM-dd"),
                 value.getOptLocalDate("opprettetDato", "yyyy-MM-dd"),
                 value.getString("status")
         )
    }
}

class JSONToSykemeldingMapper {
    fun apply(value: JSONObject?): Sykemelding {
        return if (value == null) Sykemelding(0.0f, LocalDate.now(), LocalDate.now())
        else Sykemelding(value.getFloat("grad"),
                value.getLocalDate("fom", "yyyy-MM-dd"),
                value.getLocalDate("tom", "yyyy-MM-dd"))
    }
}


fun JSONObject.getLocalDate(key: String, format: String): LocalDate {
    val rawValue: String = getString(key)
    return LocalDate.parse(rawValue, DateTimeFormatter.ofPattern(format))
}

fun JSONObject.getOptLocalDate(key: String, format: String): LocalDate? {
    val rawValue: String? = getOptString(key)
    return when (rawValue) {
        null -> null
        else -> LocalDate.parse(rawValue, DateTimeFormatter.ofPattern(format))
    }
}

fun JSONObject.getOptString(key: String): String? {
    return try {
        getString(key)
    } catch(e: Exception){
        return null
    }
}