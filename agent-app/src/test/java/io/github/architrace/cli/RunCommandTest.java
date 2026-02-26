/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.cli;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RunCommandTest {

  @InjectMocks
  private RunCommand sut;

  @Test
  void runShouldFailFastWhenConfigFileDoesNotExist() throws Exception {
    File missing = new File("build/this-config-does-not-exist.yaml");
    setField(sut, "configFile", missing);

    assertThatThrownBy(sut::run).isInstanceOf(IllegalArgumentException.class);
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
