/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.cli;

import io.github.architrace.runtime.AgentRuntimeService;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "run",
    description = "Start Architrace runtime agent"
)
public class RunCommand implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(RunCommand.class);

  private final AgentRuntimeService runtimeService = new AgentRuntimeService();

  @Option(
      names = "--config",
      required = true,
      description = "Path to YAML config file"
  )
  private File configFile;

  @Override
  public void run() {
    runtimeService.start(configFile.toPath());
    LOG.info("Architrace runtime started.");
  }
}
