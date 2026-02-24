import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    `java-library`
    jacoco
    alias(libs.plugins.spotless) apply false
    id("org.sonarqube") version "7.1.0.6387"
    id("org.graalvm.buildtools.native") version "0.10.2"
}

allprojects {
    group = "io.github.architrace.inteligence"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "jacoco")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        testImplementation(rootProject.libs.junit.jupiter)
        testImplementation(rootProject.libs.mockito.core)
        testImplementation(rootProject.libs.mockito.junit.jupiter)
        testRuntimeOnly(rootProject.libs.junit.platform.launcher)
    }

    tasks.withType<Test> {
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn(tasks.named("test"))
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.70".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.60".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "METHOD"
                    value = "COVEREDRATIO"
                    minimum = "0.70".toBigDecimal()
                }
            }
        }
    }

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/**/*.java")
            licenseHeaderFile(
                rootProject.file("license-header.txt"),
                "package "
            )
        }
    }
}

tasks.register<JavaExec>("runArchitrace") {
    group = "application"
    description = "Run Architrace CLI"
    classpath = project(":agent-app").sourceSets["main"].runtimeClasspath
    mainClass.set("io.github.architrace.MainApp")
}

tasks.register("buildRuntime") {
}

configurations.all {
    resolutionStrategy {
        force(libs.guava)
    }
}

sonar {
    properties {
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("architrace")
            mainClass.set("io.github.architrace.MainApp")
            buildArgs.add("--no-fallback")
        }
    }
}
