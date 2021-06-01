plugins {
    java
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    kotlin("jvm") version "1.4.21"
    maven
    signing
    `maven-publish`
}

group = "com.urielsalis"
version = "2.0.2"

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
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.12.3")

    testImplementation("org.junit.jupiter", "junit-jupiter", "5.7.1")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}

val sourcesJar = tasks.create("sourcesJar", Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar = tasks.create("javadocJar", Jar::class) {
    dependsOn.add(tasks.javadoc)
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "mc-crash-lib"

            from(components["java"])

            pom {
                name.set("mc-crash-lib")
                description.set("A library to parse Minecraft crashes and deobfuscate them")
                url.set("https://github.com/urielsalis/mc-crash-lib")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://www.opensource.org/licenses/mit-license.php")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/urielsalis/mc-crash-lib.git/")
                    developerConnection.set("scm:git:ssh://github.com:urielsalis/mc-crash-lib.git")
                    url.set("https://github.com/urielsalis/mc-crash-lib")
                }
                developers {
                    developer {
                        id.set("urielsalis")
                        name.set("Uriel Salischiker")
                        email.set("sonatype@urielsalis.com")
                    }
                }
            }
        }
    }

    val ossrhUsername: String? by project
    val ossrhPassword: String? by project

    repositories {
        maven(url = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }

    signing {
        sign(publishing.publications["mavenJava"])
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
