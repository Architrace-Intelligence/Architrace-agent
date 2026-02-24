# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY agent-api ./api
COPY agent-core ./core
COPY adapter-otel ./adapter-otel
COPY runtime-blueprint ./runtime-blueprint
COPY infra-db ./infra-db
COPY server ./server
COPY exporter-mermaid ./exporter-mermaid
COPY exporter-eventcatalog ./exporter-eventcatalog
COPY diff ./diff
COPY cli ./cli
COPY license-header.txt README.md ./

RUN chmod +x ./gradlew \
    && ./gradlew --no-daemon :cli:shadowJar \
    && cp cli/build/libs/*-all.jar /workspace/architrace.jar

FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S architrace && adduser -S -G architrace architrace

WORKDIR /app
COPY --from=build /workspace/architrace.jar /app/architrace.jar
RUN mkdir -p /app/data /data /tmp && chown -R architrace:architrace /app /data /tmp

VOLUME ["/tmp", "/config", "/data"]
EXPOSE 8080

ENV JAVA_HEAP_DUMP_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp" \
    JAVA_ON_OUT_OF_MEMORY_OPTS="-XX:+ExitOnOutOfMemoryError" \
    JAVA_ERROR_FILE_OPTS="-XX:ErrorFile=/tmp/java_error.log" \
    JAVA_NATIVE_MEMORY_TRACKING_OPTS="-XX:NativeMemoryTracking=summary -XX:+UnlockDiagnosticVMOptions -XX:+PrintNMTStatistics" \
    GC_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseZGC" \
    JAVA_OPTS=""

USER architrace

ENTRYPOINT ["sh", "-c", "exec java $JAVA_HEAP_DUMP_OPTS $JAVA_ON_OUT_OF_MEMORY_OPTS $JAVA_ERROR_FILE_OPTS $JAVA_NATIVE_MEMORY_TRACKING_OPTS $GC_OPTS $JAVA_OPTS -jar /app/architrace.jar \"$@\"", "entrypoint"]
CMD ["run", "--config", "/config/architrace.properties"]
