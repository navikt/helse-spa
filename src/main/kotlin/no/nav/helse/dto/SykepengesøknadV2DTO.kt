package no.nav.helse.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class SykepengesøknadV2DTO(
        val id: String,
        val type: SoknadstypeDTO,
        val aktorId: String,
        val status: SoknadsstatusDTO,
        val arbeidsgiver: ArbeidsgiverDTO,
        val arbeidsgiverForskutterer: ArbeidsgiverForskuttererDTO,
        val soktUtenlandsopphold: Boolean,
        val fom: LocalDate,
        val tom: LocalDate,
        val startSyketilfelle: LocalDate,
        val sendtNav: LocalDateTime?,
        val soknadsperioder: List<SoknadsperiodeDTO>,
        val fravar: List<FravarDTO>,
        val andreInntektskilder: List<InntektskildeDTO>
)
