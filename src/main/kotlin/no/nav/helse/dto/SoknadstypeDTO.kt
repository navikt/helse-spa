package no.nav.helse.dto

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

enum class SoknadstypeDTO {
    SELVSTENDIGE_OG_FRILANSERE,
    OPPHOLD_UTLAND,
    ARBEIDSTAKERE,
    @JsonEnumDefaultValue
    UKJENT
}
