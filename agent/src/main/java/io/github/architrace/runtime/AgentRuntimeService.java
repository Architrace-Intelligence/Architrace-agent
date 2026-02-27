/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.runtime;

import io.github.architrace.controlplane.ControlPlaneBootstrapService;
import io.github.architrace.core.config.AgentConfig;
import io.github.architrace.core.config.AgentConfigLoader;
import io.github.architrace.grpc.ControlPlaneClient;
import io.github.architrace.grpc.HealthCheckScheduler;
import io.github.architrace.otlp.OtlpTraceReceiverServer;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentRuntimeService {

  private static final Logger log = LoggerFactory.getLogger(AgentRuntimeService.class);
  private static final int OTLP_RECEIVER_PORT = 4319;

  private final AgentConfigLoader configLoader;
  private final ControlPlaneBootstrapService bootstrapService;
  private final ConcurrentHashMap<String, String> liveConfig = new ConcurrentHashMap<>();
  private final HealthCheckScheduler healthCheckScheduler;

  public AgentRuntimeService() {
    this(new AgentConfigLoader(), new ControlPlaneBootstrapService());
  }

  AgentRuntimeService(AgentConfigLoader configLoader,
                      ControlPlaneBootstrapService bootstrapService) {
    this.configLoader = configLoader;
    this.bootstrapService = bootstrapService;
    this.healthCheckScheduler = new HealthCheckScheduler();
  }

  public void start(Path configPath) {
    var config = configLoader.load(configPath);

    try (var otlpReceiver = startOtlpReceiver();
         var controlPlaneClient = bootstrapService.bootstrap(config, this::applyRemoteConfig)) {
      log.debug("Runtime resources initialized receiver=[{}]",
          otlpReceiver.getClass().getSimpleName());
      log.info("Agent=[{}] started gRPC stream to control plane=[{}]",
          config.agent().name(),
          config.control().planeBootstrap().server());
      controlPlaneClient.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Agent runtime interrupted.", e);
    } catch (Exception e) {
      throw new IllegalStateException("Agent runtime failed.", e);
    }
  }

  public RuntimeConfigSummary validateConfig(Path configPath) {
    AgentConfig config = configLoader.load(configPath);
    return new RuntimeConfigSummary(
        config.environment(),
        config.clusterId(),
        config.domainId(),
        config.namespace(),
        config.agent().name(),
        config.control().planeBootstrap().server());
  }

  public Map<String, String> liveConfigSnapshot() {
    return Map.copyOf(liveConfig);
  }

  private void applyRemoteConfig(String version, Map<String, String> entries) {
    liveConfig.putAll(entries);
    log.info("Applied remote config version {} with {} entries.", version, entries.size());
  }

  private OtlpTraceReceiverServer startOtlpReceiver() {
    OtlpTraceReceiverServer receiver = new OtlpTraceReceiverServer(OTLP_RECEIVER_PORT);
    receiver.start();
    log.info("OTLP gRPC receiver started on port {}.", OTLP_RECEIVER_PORT);
    return receiver;
  }
}
