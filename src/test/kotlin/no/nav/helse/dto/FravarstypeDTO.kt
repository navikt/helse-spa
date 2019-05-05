package no.nav.helse.dto

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

enum class FravarstypeDTO {
    FERIE,
    PERMISJON,
    UTLANDSOPPHOLD,
    UTDANNING_FULLTID,
    UTDANNING_DELTID,
    @JsonEnumDefaultValue
    UKJENT
}
