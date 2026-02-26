import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.protobuf)
}

dependencies {
    implementation(libs.guice)
    implementation(libs.slf4j.api)
    implementation(libs.picocli)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.grpc.netty.shaded)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.services)
    implementation(libs.grpc.util)
    implementation(libs.protobuf.java)
    implementation(libs.opentelemetry.proto)
    compileOnly(libs.javax.annotation.api)
    testImplementation(libs.logback.classic)
    annotationProcessor(libs.picocli.codegen)
    runtimeOnly(libs.logback.classic)
}

application {
    mainClass.set("io.github.architrace.MainApp")
}

val libsCatalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-Aproject=${project.group}/${project.name}"
    ))
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("all")
    mergeServiceFiles()
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get()
        )
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libsCatalog.findVersion("protobuf").get().requiredVersion}"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libsCatalog.findVersion("grpc").get().requiredVersion}"
        }
    }
    generateProtoTasks {
        all().forEach { task: com.google.protobuf.gradle.GenerateProtoTask ->
            task.plugins {
                create("grpc")
            }
        }
    }
}

tasks.named("build") {
    dependsOn("shadowJar")
}
