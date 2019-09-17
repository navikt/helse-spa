import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val slf4jVersion = "1.7.25"
val ktorVersion = "1.1.2"
val prometheusVersion = "0.5.0"
val gsonVersion = "2.7"
val navStreamsVersion = "d392648"
val fuelVersion = "1.15.1"
val arrowVersion = "0.9.0"

val junitJupiterVersion = "5.3.1"
val assertJVersion = "3.11.1"
val assertKVersion = "0.10"
val mainClass = "no.nav.helse.AppKt"
val jacksonVersion = "2.9.8"
val wireMockVersion = "2.19.0"
val mockkVersion="1.9"

plugins {
    kotlin("jvm") version "1.3.50"
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

dependencies {
    compile(kotlin("stdlib"))
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("net.logstash.logback:logstash-logback-encoder:5.2")
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.ktor:ktor-html-builder:$ktorVersion")
    compile("io.ktor:ktor-gson:$ktorVersion")
    compile("io.prometheus:simpleclient_common:$prometheusVersion")
    compile("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    compile("com.google.code.gson:gson:$gsonVersion")
    compile("no.nav.helse:streams:$navStreamsVersion")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    compile("no.nav.helse.sykepenger.lovverk:sykepenger-inngangsvilkar:2018-12-20-101.4f8f899")
    compile("no.nav.helse.sykepenger.lovverk:sykepenger-beregning:2018-12-20-101.1e2f74a")
    compile("no.nav:nare:dfe6569")

    compile("no.nav.helse:maksdato:89af124")
    compile("no.nav:nare-prometheus:0b41ab4")
    compile("com.github.kittinunf.fuel:fuel:$fuelVersion")

    compile("io.arrow-kt:arrow-core-data:$arrowVersion")

    testCompile("com.github.tomakehurst:wiremock:$wireMockVersion") {
        exclude(group = "junit")
    }
    testCompile("io.mockk:mockk:$mockkVersion")
    testCompile("no.nav:kafka-embedded-env:2.1.1")
    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testCompile("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testCompile("org.assertj:assertj-core:$assertJVersion")
    testCompile("com.willowtreeapps.assertk:assertk:$assertKVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    maven {
        credentials {
            username = githubUser
            password = githubPassword
        }
        setUrl("https://maven.pkg.github.com/navikt/helse-streams")
    }
    jcenter()
    mavenCentral()
    maven("http://packages.confluent.io/maven/")
    maven("https://dl.bintray.com/kotlin/ktor")
    mavenLocal()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.named<Jar>("jar") {
    baseName = "app"

    manifest {
        attributes["Main-Class"] = mainClass
        attributes["Class-Path"] = configurations["compile"].map {
            it.name
        }.joinToString(separator = " ")
    }

    doLast {
        configurations["compile"].forEach {
            val file = File("$buildDir/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}

tasks.named<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.6.1"
}
