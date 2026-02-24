# Architrace
[![CI/CD](https://github.com/godmarch33/ArchiTrace/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/godmarch33/ArchiTrace/actions/workflows/ci-cd.yml)

Architrace agent with embedded control-plane flow and OTLP trace receiver.

## Requirements
- Java 25

## Project Layout
- Single module: `agent-app`
- CLI commands: `version`, `dry-run`, `run`

## Build
```bash
./gradlew :agent-app:shadowJar
```

Artifact:
- `agent-app/build/libs/agent-app-0.1.0-all.jar`

## CLI
Show help:
```bash
java -jar agent-app/build/libs/agent-app-0.1.0-all.jar --help
```

Validate config only:
```bash
java -jar agent-app/build/libs/agent-app-0.1.0-all.jar dry-run --config ./architrace.yaml
```

Run agent:
```bash
java -jar agent-app/build/libs/agent-app-0.1.0-all.jar run --config ./architrace.yaml
```

## Runtime Flow (`run`)
When `run` starts:
1. Agent loads and validates YAML config.
2. Agent starts OTLP gRPC receiver on port `4319`.
3. Agent may start embedded control-plane gRPC server if `control.plane-bootstrap.server` points to `localhost` or `127.0.0.1`.
4. Agent registers in control-plane over gRPC stream.
5. Control-plane periodically sends `ping`; agent replies with `pong`.
6. Control-plane can send config updates over the same stream; agent applies updates in runtime.

## Configuration (YAML)
Required fields:
- `environment`: one of `DEV`, `TEST`, `STG`, `PROD`
- `clusterId`: string
- `domainId`: string
- `namespace`: string
- `agent.name`: string
- `control.plane-bootstrap.server`: `host:port`

Example `architrace.yaml`:
```yaml
environment: DEV
clusterId: cluster-1
domainId: domain-abc
namespace: team-a

agent:
  name: demo-agent

control:
  plane-bootstrap:
    server: localhost:50051
```

## OTLP Receiver
- Receiver protocol: OTLP gRPC TraceService `Export`
- Receiver listen port: `4319`

Collector should export traces to this agent endpoint, for example:
- `localhost:4319`

## Testing
Run all tests:
```bash
./gradlew test
```

Run OTLP receiver integration test only:
```bash
./gradlew :agent-app:test --tests io.github.architrace.otlp.OtlpTraceReceiverServerIntegrationTest
```

## Formatting
```bash
./gradlew spotlessApply
```

## License
Apache-2.0