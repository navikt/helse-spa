package no.nav.helse.oppslag.arbeidinntektytelse.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class YtelserDTO(val infotrygd: List<InfotrygdSakOgGrunnlagDTO>)
