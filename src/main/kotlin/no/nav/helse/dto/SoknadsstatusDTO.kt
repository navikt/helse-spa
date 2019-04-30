package no.nav.helse.dto

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

enum class SoknadsstatusDTO {
    NY,
    SENDT,
    FREMTIDIG,
    KORRIGERT,
    AVBRUTT,
    SLETTET,
    @JsonEnumDefaultValue
    UKJENT
}
