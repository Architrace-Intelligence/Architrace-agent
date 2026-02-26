/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.grpc;


import io.github.architrace.grpc.proto.AgentHealthRequest;
import io.github.architrace.grpc.proto.ControlPlaneServiceGrpc;
import io.github.architrace.testsupport.TestDataProvider;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlPlaneGrpcServerTest {

  private static final String LOOPBACK_HOST = "127.0.0.1";
  private static final String MISSING_AGENT_NAME = "missing";
  private static final long HEALTH_DEADLINE_SECONDS = 3L;
  private static final long CHANNEL_SHUTDOWN_TIMEOUT_SECONDS = 2L;
  private static final long AWAIT_THREAD_JOIN_TIMEOUT_MS = 1_000L;
  private static final int THREAD_READY_COUNT = 1;
  private static final long THREAD_READY_TIMEOUT_SECONDS = 1L;

  @Test
  void serverShouldStartServeHealthAndHandleInterruptedAwait() throws Exception {
    int port = TestDataProvider.findFreePort();
    try (ControlPlaneGrpcServer sut = new ControlPlaneGrpcServer(port)) {
      sut.start();

      ManagedChannel channel = NettyChannelBuilder.forAddress(LOOPBACK_HOST, port).usePlaintext().build();
      try {
        var stub =
            ControlPlaneServiceGrpc.newBlockingStub(channel).withDeadlineAfter(HEALTH_DEADLINE_SECONDS, TimeUnit.SECONDS);
        var response =
            stub.getAgentHealth(AgentHealthRequest.newBuilder().setAgentName(MISSING_AGENT_NAME).build());
        assertThat(response).isNotNull();
        assertThat(response.getLive()).isFalse();
      } finally {
        channel.shutdownNow();
        channel.awaitTermination(CHANNEL_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      }

      AtomicReference<Throwable> awaitFailure = new AtomicReference<>();
      CountDownLatch awaitStarted = new CountDownLatch(THREAD_READY_COUNT);
      Thread awaitThread =
          new Thread(
              () -> {
                awaitStarted.countDown();
                try {
                  sut.await();
                } catch (Throwable throwable) {
                  awaitFailure.set(throwable);
                }
              });
      awaitThread.start();
      assertThat(awaitStarted.await(THREAD_READY_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
      awaitThread.interrupt();
      awaitThread.join(AWAIT_THREAD_JOIN_TIMEOUT_MS);

      assertThat(awaitFailure.get()).isInstanceOf(IllegalStateException.class);
    }
  }
}
