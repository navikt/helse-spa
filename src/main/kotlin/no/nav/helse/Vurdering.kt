package no.nav.helse

import java.time.LocalDateTime

sealed class Vurdering<V, G>

enum class Arsak {
    SKJONN,
    DARLIG_DATA,
    MANGLENDE_DATA
}

data class Uavklart<V, G>(val begrunnelse: String,
                          val arsak: Arsak,
                          val grunnlag: G): Vurdering<V, G>()

data class Avklart<V, G>(val fastsattVerdi: V,
                         val begrunnelse: String,
                         val grunnlag: G,
                         val datoForFastsettelse: LocalDateTime,
                         val fastsattAv: String): Vurdering<V, G>()
