package no.nav.helse.behandling.mvp

import no.nav.helse.behandling.søknad.Fraværstype
import no.nav.helse.behandling.søknad.Sykepengesøknad

fun sjekkSvarISøknaden(søknad: Sykepengesøknad): List<MVPFeil> {
    return with (søknad) {
        val feil = mutableListOf<MVPFeil>()

        if (andreInntektskilder.isNotEmpty()) {
            feil.add(MVPFeil("Andre inntekter i søknaden", "Søker har svart han/hun har andre inntekter"))
        }

        if (fravær.any { it.type == Fraværstype.PERMISJON }) {
            feil.add(MVPFeil("Har permisjon", "Søker har svart han/hun har permisjon"))
        }

        if (fravær.any { it.type == Fraværstype.UTDANNING_DELTID }) {
            feil.add(MVPFeil("Har deltidstudier", "Søker har opplyst at han/hun har studier (deltid)"))
        }

        if (fravær.any { it.type == Fraværstype.UTDANNING_FULLTID }) {
            feil.add(MVPFeil("Har fulltidstudier", "Søker har opplyst at han/hun har studier (fulltid)"))
        }

        if (fravær.any { it.type == Fraværstype.UTLANDSOPPHOLD }) {
            feil.add(MVPFeil("Har utenlandsopphold", "Søker har opplyst at han/hun har utenlandsopphold"))
        }

        if (arbeidGjenopptatt != null) {
            feil.add(MVPFeil("Tilbake i jobb før tiden", "Søker har opplyst at han/hun har vært tilbake i jobb før tiden"))
        }

        if (harKorrigertArbeidstid) {
            feil.add(MVPFeil("Korrigert arbeidstid", "Søker har opplyst at han/hun har jobbet mer enn en periode i sykmeldingen tilsier"))
        }

        feil
    }
}
