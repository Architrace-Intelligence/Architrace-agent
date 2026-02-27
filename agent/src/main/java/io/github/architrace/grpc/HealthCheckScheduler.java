/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HealthCheckScheduler {

  private final HealthGrpc.HealthBlockingStub healthStub;
  private final ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(1);

  public HealthCheckScheduler() {
    ManagedChannel channel = ManagedChannelBuilder
        .forAddress("localhost", 9090)
        .usePlaintext()
        .build();

    this.healthStub = HealthGrpc.newBlockingStub(channel);
  }

  public void start() {
    scheduler.scheduleAtFixedRate(
        this::checkHealth,
        0, 15, TimeUnit.SECONDS
    );
  }

  private void checkHealth() {
    try {
      HealthCheckResponse response = healthStub
          .withDeadlineAfter(5, TimeUnit.SECONDS)
          .check(HealthCheckRequest.newBuilder()
              .setService("architrace.controlplane.v1.ControlPlaneService")
              .build());

      if (response.getStatus() == HealthCheckResponse.ServingStatus.SERVING) {
        System.out.println("Server is healthy");
      } else {
        System.out.println("Server is NOT serving");
      }
    } catch (StatusRuntimeException e) {
      System.err.println("Health check failed: " + e.getStatus());
    }
  }
}