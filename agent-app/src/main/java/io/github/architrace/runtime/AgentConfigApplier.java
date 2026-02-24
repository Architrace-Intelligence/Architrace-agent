/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.runtime;

import java.util.Map;

@FunctionalInterface
public interface AgentConfigApplier {
  void apply(String version, Map<String, String> entries);
}
