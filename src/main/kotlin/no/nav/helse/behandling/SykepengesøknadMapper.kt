package no.nav.helse.behandling

import arrow.core.Either
import no.nav.helse.Behandlingsfeil
import no.nav.helse.domain.ArbeidsgiverFraSøknad
import no.nav.helse.dto.FravarstypeDTO
import no.nav.helse.dto.SykepengesøknadV2DTO
import no.nav.helse.streams.defaultObjectMapper

fun SykepengesøknadV2DTO.mapToSykepengesøknad(): Either<Behandlingsfeil, Sykepengesøknad> {
    return if (sendtNav == null) {
        Either.Left(Behandlingsfeil.ukjentDeserialiseringsfeil(id, defaultObjectMapper.valueToTree(this), Exception("sendtNav er null")))
    } else {
        Either.Right(Sykepengesøknad(
                id = id,
                aktorId = aktorId,
                type = type.name,
                status = status.name,
                arbeidsgiver = ArbeidsgiverFraSøknad(arbeidsgiver.navn, arbeidsgiver.orgnummer),
                soktUtenlandsopphold = soktUtenlandsopphold,
                fom = fom,
                tom = tom,
                startSyketilfelle = startSyketilfelle,
                sendtNav = sendtNav,
                soknadsperioder = soknadsperioder.map { periodeDto ->
                    Søknadsperiode(
                            fom = periodeDto.fom,
                            tom = periodeDto.tom,
                            sykmeldingsgrad = periodeDto.sykmeldingsgrad)
                },
                fravær = fravar.map { fraværDto ->
                    Fravær(
                            fom = fraværDto.fom,
                            tom = fraværDto.tom,
                            type = when (fraværDto.type) {
                                FravarstypeDTO.FERIE -> Fraværstype.FERIE
                                FravarstypeDTO.PERMISJON -> Fraværstype.PERMISJON
                                FravarstypeDTO.UTLANDSOPPHOLD -> Fraværstype.UTLANDSOPPHOLD
                                FravarstypeDTO.UTDANNING_FULLTID -> Fraværstype.UTDANNING_FULLTID
                                FravarstypeDTO.UTDANNING_DELTID -> Fraværstype.UTDANNING_DELTID
                                FravarstypeDTO.UKJENT -> Fraværstype.UKJENT
                            }
                    )
                },
                andreInntektskilder = andreInntektskilder.map { inntektskildeDto ->
                    Inntektskilde(
                            type = inntektskildeDto.type,
                            sykemeldt = inntektskildeDto.sykemeldt
                    )
                }
        ))
    }
}
