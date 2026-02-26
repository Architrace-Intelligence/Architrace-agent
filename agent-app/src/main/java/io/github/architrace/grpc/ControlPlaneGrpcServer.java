/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.grpc;

import io.github.architrace.controlplane.ControlPlaneRegistry;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import io.grpc.health.v1.HealthCheckResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ControlPlaneGrpcServer implements AutoCloseable {

  private static final String HEALTH_SERVICE = "control-plane";

  private final Server server;
  private final ControlPlaneServiceImpl controlPlaneService;

  public ControlPlaneGrpcServer(int port) {
    HealthStatusManager healthStatusManager = new HealthStatusManager();
    healthStatusManager.setStatus(HEALTH_SERVICE, HealthCheckResponse.ServingStatus.SERVING);

    this.controlPlaneService = new ControlPlaneServiceImpl(new ControlPlaneRegistry());
    this.server =
        ServerBuilder.forPort(port)
            .addService(controlPlaneService)
            .addService(healthStatusManager.getHealthService())
            .addService(ProtoReflectionServiceV1.newInstance())
            .build();
  }

  public void start() {
    try {
      server.start();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to start control-plane gRPC server.", e);
    }
  }

  public void await() {
    try {
      server.awaitTermination();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for gRPC server shutdown.", e);
    }
  }

  @Override
  public void close() {
    controlPlaneService.close();
    server.shutdown();
    try {
      if (!server.awaitTermination(3, TimeUnit.SECONDS)) {
        server.shutdownNow();
      }
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      server.shutdownNow();
    }
  }
}
