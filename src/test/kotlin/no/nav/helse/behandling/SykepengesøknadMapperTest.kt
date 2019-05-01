package no.nav.helse.behandling

import no.nav.helse.Behandlingsfeil
import no.nav.helse.Either
import no.nav.helse.dto.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.LocalDate
import java.time.LocalDateTime

class SykepengesøknadMapperTest {

    @Test
    fun `sendtNav kan ikke være null`() {
        val søknad = søknadMedSendtNavSomNull()

        val actual = søknad.mapToSykepengesøknad()
        when (actual) {
            is Either.Right -> fail { "expected Either.Left" }
            is Either.Left -> assertTrue(actual.left is Behandlingsfeil.Deserialiseringsfeil)
        }
    }

    @Test
    fun `arbeidsgiverForskutterer kan være null, men skal tolkes som nei`() {
        val søknad = søknadMedArbeidsgiverForskuttererSomNull()

        val actual = søknad.mapToSykepengesøknad()
        when (actual) {
            is Either.Left -> fail { "expected Either.Right" }
            is Either.Right -> assertFalse(actual.right.arbeidsgiverForskutterer)
        }
    }

    private fun søknadMedSendtNavSomNull() =
            SykepengesøknadV2DTO(
                    id = "en id",
                    type = SoknadstypeDTO.ARBEIDSTAKERE,
                    aktorId = "1234",
                    status = SoknadsstatusDTO.SENDT,
                    arbeidsgiver = ArbeidsgiverDTO("MATBUTIKKEN AS", "123456789"),
                    arbeidsgiverForskutterer = ArbeidsgiverForskuttererDTO.JA,
                    soktUtenlandsopphold = false,
                    fom = LocalDate.now().minusMonths(1),
                    tom = LocalDate.now(),
                    startSyketilfelle = LocalDate.now().minusMonths(1),
                    sendtNav = null,
                    soknadsperioder = emptyList(),
                    fravar = emptyList(),
                    andreInntektskilder = emptyList()
            )

    private fun søknadMedArbeidsgiverForskuttererSomNull() =
            SykepengesøknadV2DTO(
                    id = "en id",
                    type = SoknadstypeDTO.ARBEIDSTAKERE,
                    aktorId = "1234",
                    status = SoknadsstatusDTO.SENDT,
                    arbeidsgiver = ArbeidsgiverDTO("MATBUTIKKEN AS", "123456789"),
                    arbeidsgiverForskutterer = null,
                    soktUtenlandsopphold = false,
                    fom = LocalDate.now().minusMonths(1),
                    tom = LocalDate.now(),
                    startSyketilfelle = LocalDate.now().minusMonths(1),
                    sendtNav = LocalDateTime.now(),
                    soknadsperioder = emptyList(),
                    fravar = emptyList(),
                    andreInntektskilder = emptyList()
            )

}
