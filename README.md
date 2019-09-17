SPA
===================

[![CircleCI](https://circleci.com/gh/navikt/helse-spa.svg?style=svg)](https://circleci.com/gh/navikt/helse-spa)

Behandler sykepengesøknader.

# Bygging

Koden bygges som et vanlig Gradle-prosjekt:

```
./gradlew build
```

Det kreves dog autentisering mot Github ettersom enkelte av pakkene vi bruker ligger på Github Package Registry.
Dette kan man gjøre ved å opprette en fil, `~/.gradle/gradle.properties` med:

```
githubUser=x-access-token
githubPassword=[token]
```

Erstatt `[token]` med et du har [opprettet for anledningen](https://github.com/settings/tokens). 
Tokenet bør scopes med bare `read:packages`, som et sikkerhetstiltak.

Eventuelt kan verdiene også settes via kommandolinjen:

```
ORG_GRADLE_PROJECT_githubUser=x-access-token ORG_GRADLE_PROJECT_githubPassword=[token] ./gradlew build
```

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #område-helse.
