/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace;

import io.github.architrace.cli.DryRunCommand;
import io.github.architrace.cli.RunCommand;
import io.github.architrace.cli.VersionCommand;
import java.util.Objects;
import java.util.function.IntConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "architrace",
    mixinStandardHelpOptions = true,
    version = "Architrace 0.1.0",
    description = "Architecture Intelligence CLI",
    subcommands = {
        VersionCommand.class,
        RunCommand.class,
        DryRunCommand.class
    }
)
public class MainApp implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(MainApp.class);

  private static IntConsumer exitHandler = System::exit;

  static void main(String[] args) {
    int exitCode = execute(args);
    exitHandler.accept(exitCode);
  }

  static int execute(String[] args) {
    CommandLine cmd = new CommandLine(new MainApp());

    cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
      log.error("Execution failed: {}", ex.getMessage(), ex);
      return CommandLine.ExitCode.SOFTWARE;
    });

    cmd.setParameterExceptionHandler((ex, args1) -> {
      System.err.println("ERROR: " + ex.getMessage());
      ex.printStackTrace();
      ex.getCommandLine().usage(System.err);
      return CommandLine.ExitCode.SOFTWARE;
    });

    return cmd.execute(args);
  }

  static void setExitHandler(IntConsumer newHandler) {
    exitHandler = Objects.requireNonNull(newHandler);
  }

  static void resetExitHandler() {
    exitHandler = System::exit;
  }

  @Override
  public void run() {
    log.info("No command specified. Use --help for usage information.");
  }
}