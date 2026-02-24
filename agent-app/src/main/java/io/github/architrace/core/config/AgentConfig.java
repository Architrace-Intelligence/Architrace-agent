/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentConfig(
    Environment environment,
    String clusterId,
    String domainId,
    String namespace,
    Agent agent,
    Control control) {

  public enum Environment {
    DEV,
    TEST,
    STG,
    PROD
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Agent(String name) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Control(@JsonProperty("plane-bootstrap") PlaneBootstrap planeBootstrap) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PlaneBootstrap(String server) {
  }
}
