package no.nav.helse

import no.nav.helse.behandling.Sykepengesøknad
import no.nav.helse.behandling.SykepengesøknadV1DTO
import no.nav.helse.streams.defaultObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class LegacyTopicParseTest {

    @Test
    fun `try to parse legacy-format`() {
        val maybeSøknad: Either<Behandlingsfeil, SykepengesøknadV1DTO> = deserializeSykepengesøknadV1(defaultObjectMapper.readTree(json))

        when (maybeSøknad) {
            is Either.Left -> fail("should parse well")
            is Either.Right -> assertEquals(maybeSøknad.right.aktorId, "1000011527909")
        }
    }
}

val json = """
    {
  "aktorId": "1000011527909",
  "arbeidsgiver": "STRYN OMSORGSSENTER",
  "arbeidssituasjon": "ARBEIDSTAKER",
  "fom": "2018-11-27",
  "id": "f7bd9454-bdcc-4fbd-bba5-39d07d271108",
  "innsendtDato": "2018-12-10",
  "korrigerer": null,
  "korrigertAv": null,
  "opprettetDato": "2018-12-06",
  "soknadPerioder": [
    {
      "fom": "2018-11-27",
      "grad": 100,
      "tom": "2018-12-05"
    }
  ],
  "type": "ARBEIDSTAKERE",
  "sporsmal": [
    {
      "id": "35206",
      "kriterieForVisningAvUndersporsmal": null,
      "max": null,
      "min": null,
      "sporsmalstekst": "Jeg vet at dersom jeg gir uriktige opplysninger, eller holder tilbake opplysninger som har betydning for min rett til sykepenger, kan pengene holdes tilbake eller kreves tilbake, og/eller det kan medfÃ¸re straffeansvar. Jeg er ogsÃ¥ klar over at jeg mÃ¥ melde fra til NAV dersom jeg i sykmeldingsperioden satt i varetekt, sonet straff eller var under forvaring.",
      "svar": [
        {
          "verdi": "CHECKED"
        }
      ],
      "svartype": "CHECKBOX_PANEL",
      "tag": "ANSVARSERKLARING",
      "undersporsmal": [],
      "undertekst": null
    },
    {
      "id": "35207",
      "kriterieForVisningAvUndersporsmal": "JA",
      "max": null,
      "min": null,
      "sporsmalstekst": "Vi har registrert at du ble sykmeldt tirsdag 27. november 2018. Brukte du egenmeldinger og/eller var du sykmeldt i perioden 11. - 26. november 2018?",
      "svar": [
        {
          "verdi": "NEI"
        }
      ],
      "svartype": "JA_NEI",
      "tag": "EGENMELDINGER",
      "undersporsmal": [
        {
          "id": "35208",
          "kriterieForVisningAvUndersporsmal": null,
          "max": "2018-11-26",
          "min": "2018-05-27",
          "sporsmalstekst": "Hvilke dager fÃ¸r 27. november 2018 var du borte fra jobb?",
          "svar": [],
          "svartype": "PERIODER",
          "tag": "EGENMELDINGER_NAR",
          "undersporsmal": [],
          "undertekst": null
        }
      ],
      "undertekst": null
    },
    {
      "id": "35209",
      "kriterieForVisningAvUndersporsmal": "JA",
      "max": null,
      "min": null,
      "sporsmalstekst": "Var du tilbake i fullt arbeid hos STRYN OMSORGSSENTER fÃ¸r 6. desember 2018?",
      "svar": [
        {
          "verdi": "NEI"
        }
      ],
      "svartype": "JA_NEI",
      "tag": "TILBAKE_I_ARBEID",
      "undersporsmal": [
        {
          "id": "35210",
          "kriterieForVisningAvUndersporsmal": null,
          "max": "2018-12-05",
          "min": "2018-11-27",
          "sporsmalstekst": "Fra hvilken dato ble arbeidet gjenopptatt?",
          "svar": [],
          "svartype": "DATO",
          "tag": "TILBAKE_NAR",
          "undersporsmal": [],
          "undertekst": null
        }
      ],
      "undertekst": null
    },
    {
      "id": "35211",
      "kriterieForVisningAvUndersporsmal": "JA",
      "max": null,
      "min": null,
      "sporsmalstekst": "I perioden 27. november - 5. desember 2018 var du 100 % sykmeldt fra STRYN OMSORGSSENTER. Jobbet du noe i denne perioden?",
      "svar": [
        {
          "verdi": "NEI"
        }
      ],
      "svartype": "JA_NEI",
      "tag": "JOBBET_DU_100_PROSENT_0",
      "undersporsmal": [
        {
          "id": "35212",
          "kriterieForVisningAvUndersporsmal": null,
          "max": "150",
          "min": "1",
          "sporsmalstekst": "Hvor mange timer jobbet du per uke fÃ¸r du ble sykmeldt?",
          "svar": [],
          "svartype": "TALL",
          "tag": "HVOR_MANGE_TIMER_PER_UKE_0",
          "undersporsmal": [],
          "undertekst": "timer per uke"
        },
        {
          "id": "35213",
          "kriterieForVisningAvUndersporsmal": null,
          "max": null,
          "min": null,
          "sporsmalstekst": "Hvor mye jobbet du totalt 27. november - 5. desember 2018 hos STRYN OMSORGSSENTER?",
          "svar": [],
          "svartype": "RADIO_GRUPPE_TIMER_PROSENT",
          "tag": "HVOR_MYE_HAR_DU_JOBBET_0",
          "undersporsmal": [
            {
              "id": "35214",
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "max": null,
              "min": null,
              "sporsmalstekst": "prosent",
              "svar": [
                {
                  "verdi": "CHECKED"
                }
              ],
              "svartype": "RADIO",
              "tag": "HVOR_MYE_PROSENT_0",
              "undersporsmal": [
                {
                  "id": "35215",
                  "kriterieForVisningAvUndersporsmal": null,
                  "max": "99",
                  "min": "1",
                  "sporsmalstekst": null,
                  "svar": [],
                  "svartype": "TALL",
                  "tag": "HVOR_MYE_PROSENT_VERDI_0",
                  "undersporsmal": [],
                  "undertekst": "prosent"
                }
              ],
              "undertekst": null
            },
            {
              "id": "35216",
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "max": null,
              "min": null,
              "sporsmalstekst": "timer",
              "svar": [],
              "svartype": "RADIO",
              "tag": "HVOR_MYE_TIMER_0",
              "undersporsmal": [
                {
                  "id": "35217",
                  "kriterieForVisningAvUndersporsmal": null,
                  "max": "193",
                  "min": "1",
                  "sporsmalstekst": null,
                  "svar": [],
                  "svartype": "TALL",
                  "tag": "HVOR_MYE_TIMER_VERDI_0",
                  "undersporsmal": [],
                  "undertekst": "timer totalt"
                }
              ],
              "undertekst": null
            }
          ],
          "undertekst": null
        }
      ],
      "undertekst": null
    },
    {
      "id": "35218",
      "kriterieForVisningAvUndersporsmal": "JA",
      "max": null,
      "min": null,
      "sporsmalstekst": "Har du hatt ferie, permisjon eller oppholdt deg utenfor Norge i perioden 27. november - 5. desember 2018?",
      "svar": [
        {
          "verdi": "NEI"
        }
      ],
      "svartype": "JA_NEI",
      "tag": "FERIE_PERMISJON_UTLAND",
      "undersporsmal": [
        {
          "id": "35219",
          "kriterieForVisningAvUndersporsmal": null,
          "max": null,
          "min": null,
          "sporsmalstekst": "Kryss av alt som gjelder deg:",
          "svar": [],
          "svartype": "CHECKBOX_GRUPPE",
          "tag": "FERIE_PERMISJON_UTLAND_HVA",
          "undersporsmal": [
            {
              "id": "35220",
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "max": null,
              "min": null,
              "sporsmalstekst": "Jeg tok ut ferie",
              "svar": [],
              "svartype": "CHECKBOX",
              "tag": "FERIE",
              "undersporsmal": [
                {
                  "id": "35221",
                  "kriterieForVisningAvUndersporsmal": null,
                  "max": "2018-12-05",
                  "min": "2018-11-27",
                  "sporsmalstekst": null,
                  "svar": [],
                  "svartype": "PERIODER",
                  "tag": "FERIE_NAR",
                  "undersporsmal": [],
                  "undertekst": null
                }
              ],
              "undertekst": null
            },
            {
              "id": "35222",
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "max": null,
              "min": null,
              "sporsmalstekst": "Jeg hadde permisjon",
              "svar": [],
              "svartype": "CHECKBOX",
              "tag": "PERMISJON",
              "undersporsmal": [
                {
                  "id": "35223",
                  "kriterieForVisningAvUndersporsmal": null,
                  "max": "2018-12-05",
                  "min": "2018-11-27",
                  "sporsmalstekst": null,
                  "svar": [],
                  "svartype": "PERIODER",
                  "tag": "PERMISJON_NAR",
                  "undersporsmal": [],
                  "undertekst": null
                }
              ],
              "undertekst": null
            },
            {
              "id": "35224",
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "max": null,
              "min": null,
              "sporsmalstekst": "Jeg var utenfor Norge",
              "svar": [],
              "svartype": "CHECKBOX",
              "tag": "UTLAND",
              "undersporsmal": [
                {
                  "id": "35225",
                  "kriterieForVisningAvUndersporsmal": null,
                  "max": "2018-12-05",
                  "min": "2018-11-27",
                  "sporsmalstekst": null,
                  "svar": [],
                  "svartype": "PERIODER",
                  "tag": "UTLAND_NAR",
                  "undersporsmal": [],
                  "undertekst": null
                }
              ],
              "undertekst": null
            }
          ],
          "undertekst": null
        }
      ],
      "undertekst": null
    },
    {
      "id": "35226",
      "kriterieForVisningAvUndersporsmal": "JA",
      "max": null,
      "min": null,
      "sporsmalstekst": "Har du andre inntektskilder, eller jobber du for andre enn STRYN OMSORGSSENTER?",
      "svar": [
        {
          "verdi": "NEI"
        }
      ],
      "svartype": "JA_NEI",
      "tag": "ANDRE_INNTEKTSKILDER",
      "undersporsmal": [
        {
          "id": "35227",
          "kriterieForVisningAvUndersporsmal": null,
          "max": null,
          "min": null,
          "sporsmalstekst": "Hvilke andre inntektskilder har du?",
          "svar": [],
          "svartype": "CHECKBOX_GRUPPE",
          "tag": "HVILKE_ANDRE_INNTEKTSKILDER",
          "undersporsmal": [
            {
              "id": "35228",
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "max": null,
              "min": null,
              "sporsmalstekst": "Andre arbeidsforhold",
              "svar": [],
              "svartype": "CHECKBOX",
              "tag": "INNTEKTSKILDE_ANDRE_ARBEIDSFORHOLD",
              "undersporsmal": [
                {
                  "id": "35229",
                  "kriterieForVisningAvUndersporsmal": null,
                  "max": null,
                  "min": null,
                  "sporsmalstekst": "Er du sykmeldt fra dette?",
                  "svar": [],
                  "svartype": "JA_NEI",
                  "tag": "INNTEKTSKILDE_ANDRE_ARBEIDSFORHOLD_ER_DU_SYKMELDT",
                  "undersporsmal": [],
                  "undertekst": null
                }
              ],
              "undertekst": null
            },
            {
              "id": "35230",
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "max": null,
              "min": null,
              "sporsmalstekst": "Selvstendig nÃ¦ringsdrivende",
              "svar": [],
              "svartype": "CHECKBOX",
              "tag": "INNTEKTSKILDE_SELVSTENDIG",
              "undersporsmal": [
                {
                  "id": "35231",
                  "kriterieForVisningAvUndersporsmal": null,
                  "max": null,
                  "min": null,
                  "sporsmalstekst": "Er du sykmeldt fra dette?",
                  "svar": [],
                  "svartype": "JA_NEI",
                  "tag": "INNTEKTSKILDE_SELVSTENDIG_ER_DU_SYKMELDT",
                  "undersporsmal": [],
                  "undertekst": null
                }
              ],
              "undertekst": null
            },
            {
              "id": "35232",
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "max": null,
              "min": null,
              "sporsmalstekst": "Selvstendig nÃ¦ringsdrivende dagmamma",
              "svar": [],
              "svartype": "CHECKBOX",
              "tag": "INNTEKTSKILDE_SELVSTENDIG_DAGMAMMA",
              "undersporsmal": [
                {
                  "id": "35233",
                  "kriterieForVisningAvUndersporsmal": null,
                  "max": null,
                  "min": null,
                  "sporsmalstekst": "Er du sykmeldt fra dette?",
                  "svar": [],
                  "svartype": "JA_NEI",
                  "tag": "INNTEKTSKILDE_SELVSTENDIG_DAGMAMMA_ER_DU_SYKMELDT",
                  "undersporsmal": [],
                  "undertekst": null
                }
              ],
              "undertekst": null
            },
            {
              "id": "35234",
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "max": null,
              "min": null,
              "sporsmalstekst": "Jordbruker / Fisker / ReindriftsutÃ¸ver",
              "svar": [],
              "svartype": "CHECKBOX",
              "tag": "INNTEKTSKILDE_JORDBRUKER",
              "undersporsmal": [
                {
                  "id": "35235",
                  "kriterieForVisningAvUndersporsmal": null,
                  "max": null,
                  "min": null,
                  "sporsmalstekst": "Er du sykmeldt fra dette?",
                  "svar": [],
                  "svartype": "JA_NEI",
                  "tag": "INNTEKTSKILDE_JORDBRUKER_ER_DU_SYKMELDT",
                  "undersporsmal": [],
                  "undertekst": null
                }
              ],
              "undertekst": null
            },
            {
              "id": "35236",
              "kriterieForVisningAvUndersporsmal": "CHECKED",
              "max": null,
              "min": null,
              "sporsmalstekst": "Frilanser",
              "svar": [],
              "svartype": "CHECKBOX",
              "tag": "INNTEKTSKILDE_FRILANSER",
              "undersporsmal": [
                {
                  "id": "35237",
                  "kriterieForVisningAvUndersporsmal": null,
                  "max": null,
                  "min": null,
                  "sporsmalstekst": "Er du sykmeldt fra dette?",
                  "svar": [],
                  "svartype": "JA_NEI",
                  "tag": "INNTEKTSKILDE_FRILANSER_ER_DU_SYKMELDT",
                  "undersporsmal": [],
                  "undertekst": null
                }
              ],
              "undertekst": null
            },
            {
              "id": "35238",
              "kriterieForVisningAvUndersporsmal": null,
              "max": null,
              "min": null,
              "sporsmalstekst": "Annet",
              "svar": [],
              "svartype": "CHECKBOX",
              "tag": "INNTEKTSKILDE_ANNET",
              "undersporsmal": [],
              "undertekst": null
            }
          ],
          "undertekst": "Du trenger ikke oppgi andre ytelser fra NAV"
        }
      ],
      "undertekst": null
    },
    {
      "id": "35239",
      "kriterieForVisningAvUndersporsmal": "JA",
      "max": null,
      "min": null,
      "sporsmalstekst": "Har du vÃ¦rt under utdanning i lÃ¸pet av perioden 27. november - 5. desember 2018?",
      "svar": [
        {
          "verdi": "NEI"
        }
      ],
      "svartype": "JA_NEI",
      "tag": "UTDANNING",
      "undersporsmal": [
        {
          "id": "35240",
          "kriterieForVisningAvUndersporsmal": null,
          "max": "2018-12-05",
          "min": null,
          "sporsmalstekst": "NÃ¥r startet du pÃ¥ utdanningen?",
          "svar": [],
          "svartype": "DATO",
          "tag": "UTDANNING_START",
          "undersporsmal": [],
          "undertekst": null
        },
        {
          "id": "35241",
          "kriterieForVisningAvUndersporsmal": null,
          "max": null,
          "min": null,
          "sporsmalstekst": "Er utdanningen et fulltidsstudium?",
          "svar": [],
          "svartype": "JA_NEI",
          "tag": "FULLTIDSSTUDIUM",
          "undersporsmal": [],
          "undertekst": null
        }
      ],
      "undertekst": null
    },
    {
      "id": "35242",
      "kriterieForVisningAvUndersporsmal": null,
      "max": null,
      "min": null,
      "sporsmalstekst": "VÃ¦r klar over at:",
      "svar": [],
      "svartype": "IKKE_RELEVANT",
      "tag": "VAER_KLAR_OVER_AT",
      "undersporsmal": [],
      "undertekst": "<ul><li>rett til sykepenger forutsetter at du er borte fra arbeid pÃ¥ grunn av egen sykdom. Sosiale eller Ã¸konomiske problemer gir ikke rett til sykepenger</li><li>du kan miste retten til sykepenger hvis du uten rimelig grunn nekter Ã¥ opplyse om egen funksjonsevne eller nekter Ã¥ ta imot tilbud om behandling og/eller tilrettelegging</li><li>sykepenger utbetales i maksimum 52 uker, ogsÃ¥ for gradert (delvis) sykmelding</li><li>fristen for Ã¥ sÃ¸ke sykepenger er som hovedregel 3 mÃ¥neder</li></ul>"
    },
    {
      "id": "35243",
      "kriterieForVisningAvUndersporsmal": null,
      "max": null,
      "min": null,
      "sporsmalstekst": "Jeg har lest all informasjonen jeg har fÃ¥tt i sÃ¸knaden og bekrefter at opplysningene jeg har gitt er korrekte.",
      "svar": [
        {
          "verdi": "CHECKED"
        }
      ],
      "svartype": "CHECKBOX_PANEL",
      "tag": "BEKREFT_OPPLYSNINGER",
      "undersporsmal": [],
      "undertekst": null
    },
    {
      "id": "35244",
      "kriterieForVisningAvUndersporsmal": null,
      "max": null,
      "min": null,
      "sporsmalstekst": "Betaler arbeidsgiveren lÃ¸nnen din nÃ¥r du er syk?",
      "svar": [],
      "svartype": "RADIO_GRUPPE",
      "tag": "BETALER_ARBEIDSGIVER",
      "undersporsmal": [
        {
          "id": "35245",
          "kriterieForVisningAvUndersporsmal": null,
          "max": null,
          "min": null,
          "sporsmalstekst": "Ja",
          "svar": [],
          "svartype": "RADIO",
          "tag": "BETALER_ARBEIDSGIVER_JA",
          "undersporsmal": [],
          "undertekst": "Arbeidsgiveren din mottar kopi av sÃ¸knaden du sender til NAV."
        },
        {
          "id": "35246",
          "kriterieForVisningAvUndersporsmal": null,
          "max": null,
          "min": null,
          "sporsmalstekst": "Nei",
          "svar": [],
          "svartype": "RADIO",
          "tag": "BETALER_ARBEIDSGIVER_NEI",
          "undersporsmal": [],
          "undertekst": "SÃ¸knaden sendes til NAV. Arbeidsgiveren din fÃ¥r ikke kopi."
        },
        {
          "id": "35247",
          "kriterieForVisningAvUndersporsmal": null,
          "max": null,
          "min": null,
          "sporsmalstekst": "Vet ikke",
          "svar": [
            {
              "verdi": "CHECKED"
            }
          ],
          "svartype": "RADIO",
          "tag": "BETALER_ARBEIDSGIVER_VET_IKKE",
          "undersporsmal": [],
          "undertekst": "Siden du ikke vet svaret pÃ¥ dette spÃ¸rsmÃ¥let, vil arbeidsgiveren din motta en kopi av sÃ¸knaden du sender til NAV."
        }
      ],
      "undertekst": null
    }
  ],
  "startSykeforlop": "2018-11-27",
  "status": "SENDT",
  "sykmeldingId": "03f4c884-cce1-493c-be74-d48b937e11e8",
  "sykmeldingUtskrevet": null,
  "tom": "2018-12-05"
}
""".trimIndent()
