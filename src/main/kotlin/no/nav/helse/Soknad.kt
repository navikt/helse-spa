package no.nav.helse

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year

data class Sykepengesøknad(val aktørId: String,
                           val soktUtenlandsopphold: Boolean,
                           val fom: LocalDate,
                           val tom: LocalDate,
                           val startSyketilfelle: LocalDate,
                           val sendtNav: LocalDateTime,
                           val soknadsperioder: List<Søknadsperiode>,

                           val faktagrunnlag: Faktagrunnlag,
                           val harVurdertInntekt: Boolean,
                           val andreYtelser: List<String>)

data class Søknadsperiode(val fom: LocalDate,
                          val tom: LocalDate,
                          val sykmeldingsgrad: Int)

data class Faktagrunnlag(val tps: Tpsfakta, val aareg: Aaregfakta, val aordningen: Aordningenfakta,
                         val inntektsmelding: Inntektsmeldingfakta)

data class Tpsfakta(val fødselsdato: LocalDate, val bostedland: String) {

    fun alder(): Int {
        val age = Year.now().value - fødselsdato.year
        return if (LocalDate.now().withYear(fødselsdato.year) >= fødselsdato) age else age - 1
    }
}

data class Aaregfakta(val arbeidsforhold: List<Arbeidsforhold>)
data class Arbeidsforhold(val arbeidsgiver: Arbeidsgiver, val arbeidsavtale: List<Arbeidsavtale>)
data class Arbeidsgiver(val orgnummer: String)
data class Arbeidsavtale(val fomGyldighetsperiode: LocalDate)

data class Aordningenfakta(val perioder: List<Periode>)
data class Periode(val periode: LocalDate, val beløp: Long)

data class Inntektsmeldingfakta(val beløp: Long, val omregnetÅrsinntekt: Long = beløp * 12)
