/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.controlplane;

import io.github.architrace.core.config.AgentConfig;
import io.github.architrace.grpc.AgentControlPlaneClient;
import io.github.architrace.runtime.AgentConfigApplier;

public class ControlPlaneBootstrapService {

  public AgentControlPlaneClient bootstrap(AgentConfig config, AgentConfigApplier configApplier) {
    String agentName = config.agent().name();
    String controlPlaneServer = config.control().planeBootstrap().server();
    AgentControlPlaneClient client =
        new AgentControlPlaneClient(controlPlaneServer, agentName, configApplier);
    client.start();
    return client;
  }
}
