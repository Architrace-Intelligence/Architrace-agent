plugins {
    `java-library`
    alias(libs.plugins.protobuf)
}

dependencies {
    api(libs.grpc.stub)
    api(libs.grpc.protobuf)
    api(libs.protobuf.java)
    api(libs.jackson.databind)
    api(libs.jakarta.validation.api)
    api(libs.swagger.annotations)
}
