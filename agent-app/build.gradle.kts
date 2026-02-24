import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    alias(libs.plugins.shadow)
    id("com.google.protobuf") version "0.9.4"
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
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
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
