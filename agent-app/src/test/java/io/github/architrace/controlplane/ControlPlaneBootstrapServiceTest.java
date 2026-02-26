/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.controlplane;


import io.github.architrace.grpc.AgentControlPlaneClient;
import io.github.architrace.grpc.ControlPlaneGrpcServer;
import io.github.architrace.testsupport.TestDataProvider;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlPlaneBootstrapServiceTest {

  private static final String LOOPBACK_HOST = "127.0.0.1";
  private static final String AGENT_NAME = "agent-a";
  private static final int INITIAL_CONFIG_LATCH_COUNT = 1;
  private static final long CONFIG_WAIT_TIMEOUT_SECONDS = 5L;

  @Test
  void bootstrapShouldCreateStartAndReturnClient() throws Exception {
    int port = TestDataProvider.findFreePort();
    String endpoint = LOOPBACK_HOST + ":" + port;
    var config = TestDataProvider.createAgentConfig(endpoint, AGENT_NAME);
    CountDownLatch applied = new CountDownLatch(INITIAL_CONFIG_LATCH_COUNT);
    AtomicReference<Map<String, String>> entriesRef = new AtomicReference<>();

    try (ControlPlaneGrpcServer server = new ControlPlaneGrpcServer(port)) {
      server.start();
      ControlPlaneBootstrapService sut = new ControlPlaneBootstrapService();

      try (AgentControlPlaneClient client =
          sut.bootstrap(
              config,
              (version, entries) -> {
                entriesRef.set(entries);
                applied.countDown();
              })) {
        assertThat(client).isNotNull();
        assertThat(applied.await(CONFIG_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(entriesRef.get()).containsEntry("agent.mode", "managed");
      }
    }
  }
}
