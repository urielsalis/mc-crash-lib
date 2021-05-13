plugins {
    java
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    kotlin("jvm") version "1.4.21"
    maven
}

group = "me.urielsalis"
version = "1.0-SNAPSHOT"

val arrowVersion = "0.10.4"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.arrow-kt", "arrow-core", arrowVersion)
    implementation("io.arrow-kt", "arrow-syntax", arrowVersion)
    implementation("io.arrow-kt", "arrow-fx", arrowVersion)
    implementation("com.guardsquare", "proguard-retrace", "7.1.0-beta3")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.12.+")

    testImplementation("org.junit.jupiter", "junit-jupiter", "5.7.1")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
