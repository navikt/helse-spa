package no.nav.helse.dto

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue

enum class ArbeidsgiverForskuttererDTO {
    JA,
    NEI,
    VET_IKKE,
    @JsonEnumDefaultValue
    UKJENT
}
