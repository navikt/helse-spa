package no.nav.helse.fastsetting

import java.time.LocalDateTime

sealed class Vurdering<out V, out G>(val begrunnelse: String, val grunnlag: G, val vurderingstidspunkt: LocalDateTime = LocalDateTime.now()) {
    class Avklart<V, G>(val fastsattVerdi: V,
                        begrunnelse: String,
                        grunnlag: G,
                        val fastsattAv: String): Vurdering<V, G>(begrunnelse, grunnlag) {

        override fun toString(): String {
            return "Avklart(fastsattVerdi=$fastsattVerdi, begrunnelse=$begrunnelse)"
        }

    }
    class Uavklart<G>(val årsak: Årsak,
                      begrunnelse: String,
                      grunnlag: G): Vurdering<Nothing, G>(begrunnelse, grunnlag) {

        enum class Årsak {
            KREVER_SKJØNNSMESSIG_VURDERING,
            DÅRLIG_DATAGRUNNLAG,
            MANGELFULL_DATAGRUNNLAG
        }

        override fun toString(): String {
            return "Uavklart(årsak=$årsak, begrunnelse=$begrunnelse)"
        }


    }
}
