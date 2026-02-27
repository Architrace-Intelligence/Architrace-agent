/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.controlplane;

import io.github.architrace.core.config.AgentConfig;
import io.github.architrace.grpc.ControlPlaneClient;
import io.github.architrace.runtime.AgentConfigApplier;

public class ControlPlaneBootstrapService {

  public ControlPlaneClient bootstrap(AgentConfig config, AgentConfigApplier configApplier) {
    var agentName = config.agent().name();
    var controlPlaneServer = config.control().planeBootstrap().server();
    var client = new ControlPlaneClient(controlPlaneServer, agentName, configApplier);

    client.start();

    return client;
  }
}
