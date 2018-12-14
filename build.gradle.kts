val slf4jVersion = "1.7.25"
val ktorVersion = "1.0.0"
val prometheusVersion = "0.5.0"
val gsonVersion = "2.7"
val navStreamsVersion = "10"

val junitJupiterVersion = "5.3.1"
val assertJVersion = "3.11.1"
val mainClass = "no.nav.helse.AppKt"

plugins {
    application
    kotlin("jvm") version "1.3.11"
    id("com.github.johnrengelman.shadow") version "4.0.3"
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

application {
    mainClassName = "$mainClass"
}

sourceSets {
    getByName("main").java.srcDirs("src/main/kotlin")
    getByName("test").java.srcDirs("src/test/kotlin")
}

dependencies {
    compile(kotlin("stdlib"))
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("net.logstash.logback:logstash-logback-encoder:5.2")
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.prometheus:simpleclient_common:$prometheusVersion")
    compile("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    compile("com.google.code.gson:gson:$gsonVersion")
    compile("no.nav.helse:streams:$navStreamsVersion")

    testCompile ("no.nav:kafka-embedded-env:2.0.1")
    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testCompile("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testCompile("org.assertj:assertj-core:$assertJVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

repositories {
    jcenter()
    mavenCentral()
    maven("http://packages.confluent.io/maven/")
    maven("https://dl.bintray.com/kotlin/ktor")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "4.10.2"
}
