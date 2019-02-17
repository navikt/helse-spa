package no.nav.helse

import java.time.LocalDateTime

sealed class Vurdering<out V, out G>(val begrunnelse: String, val grunnlag: G, val vurderingstidspunkt: LocalDateTime = LocalDateTime.now()) {
    class Avklart<V, G>(val fastsattVerdi: V,
                        begrunnelse: String,
                        grunnlag: G,
                        val fastsattAv: String): Vurdering<V, G>(begrunnelse, grunnlag)
    class Uavklart<G>(val arsak: Uavklart.Arsak,
                      begrunnelse: String,
                      grunnlag: G): Vurdering<Nothing, G>(begrunnelse, grunnlag) {

        enum class Arsak {
            SKJONN,
            DARLIG_DATA,
            MANGLENDE_DATA
        }
    }
}
