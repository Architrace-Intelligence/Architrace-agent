/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.architrace.testsupport.TestDataProvider;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentRuntimeServiceTest {

  @TempDir
  Path tempDir;

  private AgentRuntimeService sut;

  @BeforeEach
  void setUp() {
    sut = new AgentRuntimeService();
  }

  @Test
  void validateConfigShouldMapLoadedConfigToSummary() throws Exception {
    Path config =
        TestDataProvider.copyResourceToTemp(
            tempDir,
            "testdata/config/valid-agent-config.json",
            "valid-agent-config.json");

    RuntimeConfigSummary summary = sut.validateConfig(config);

    assertThat(summary.environment()).isEqualTo(io.github.architrace.core.config.AgentConfig.Environment.DEV);
    assertThat(summary.clusterId()).isEqualTo("cluster-1");
    assertThat(summary.domainId()).isEqualTo("domain-1");
    assertThat(summary.namespace()).isEqualTo("team-a");
    assertThat(summary.agentName()).isEqualTo("demo-agent");
    assertThat(summary.controlPlaneServer()).isEqualTo("localhost:50051");
  }

  @Test
  void liveConfigSnapshotShouldBeImmutableAndContainAppliedEntries() throws Exception {
    Method applyRemoteConfig = AgentRuntimeService.class.getDeclaredMethod("applyRemoteConfig", String.class, Map.class);
    applyRemoteConfig.setAccessible(true);
    applyRemoteConfig.invoke(sut, "2", Map.of("feature.enabled", "true"));

    Map<String, String> snapshot = sut.liveConfigSnapshot();
    assertThat(snapshot).containsEntry("feature.enabled", "true");
    assertThatThrownBy(() -> snapshot.put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void maybeStartEmbeddedControlPlaneShouldReturnNoopForRemoteHost() throws Exception {
    Method maybeStartEmbedded =
        AgentRuntimeService.class.getDeclaredMethod("maybeStartEmbeddedControlPlane", InetSocketAddress.class);
    maybeStartEmbedded.setAccessible(true);

    AutoCloseable closeable =
        (AutoCloseable) maybeStartEmbedded.invoke(sut, new InetSocketAddress("example.com", 50051));

    closeable.close();
    assertThat(closeable).isNotNull();
  }

  @Test
  void maybeStartEmbeddedControlPlaneShouldStartAndStopForLocalhost() throws Exception {
    Method maybeStartEmbedded =
        AgentRuntimeService.class.getDeclaredMethod("maybeStartEmbeddedControlPlane", InetSocketAddress.class);
    maybeStartEmbedded.setAccessible(true);

    int port = TestDataProvider.findFreePort();
    AutoCloseable closeable =
        (AutoCloseable) maybeStartEmbedded.invoke(sut, new InetSocketAddress("localhost", port));

    closeable.close();
    assertThat(closeable).isNotNull();
  }
}
