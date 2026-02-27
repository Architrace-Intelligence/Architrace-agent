/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class AgentConfigLoader {

  private final ObjectMapper objectMapper;

  public AgentConfigLoader() {
    this.objectMapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();
  }

  public AgentConfig load(Path configPath) {
    Objects.requireNonNull(configPath, "configPath is required");
    if (!Files.exists(configPath)) {
      throw new IllegalArgumentException("Config file does not exist: " + configPath);
    }

    try (InputStream inputStream = Files.newInputStream(configPath)) {
      AgentConfig config = objectMapper.readValue(inputStream, AgentConfig.class);
      validate(config);

      return config;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read config file: " + configPath, e);
    }
  }

  private static void validate(AgentConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("Config is empty.");
    }

    if (config.environment() == null) {
      throw new IllegalArgumentException(
          "Missing required config field: environment (allowed: DEV, TEST, STG, PROD)");
    }

    if (isBlank(config.clusterId())) {
      throw new IllegalArgumentException("Missing required config field: clusterId");
    }

    if (isBlank(config.domainId())) {
      throw new IllegalArgumentException("Missing required config field: domainId");
    }

    if (isBlank(config.namespace())) {
      throw new IllegalArgumentException("Missing required config field: namespace");
    }

    if (config.agent() == null || isBlank(config.agent().name())) {
      throw new IllegalArgumentException("Missing required config field: agent.name");
    }

    if (config.control() == null
        || config.control().planeBootstrap() == null
        || isBlank(config.control().planeBootstrap().server())) {
      throw new IllegalArgumentException(
          "Missing required config field: control.plane-bootstrap.server");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
