/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.grpc;


import io.github.architrace.grpc.proto.AgentEvent;
import io.github.architrace.grpc.proto.ControlPlaneEvent;
import io.github.architrace.grpc.proto.ControlPlanePing;
import io.github.architrace.testsupport.GrpcEventDataFactory;
import io.grpc.stub.StreamObserver;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentControlPlaneClientTest {

  private static final String TEST_ENDPOINT = "127.0.0.1:65530";
  private static final String TEST_AGENT = "agent-a";
  private static final long INBOUND_PING_ID = 9L;
  private static final long INBOUND_SENT_AT_EPOCH_MS = 10L;
  private static final int EXPECTED_SINGLE_EVENT = 1;
  private static final String EXPECTED_CONFIG_VERSION = "42";

  @Test
  void awaitShouldThrowWhenStreamFailureIsSet() throws Exception {
    try (AgentControlPlaneClient sut = new AgentControlPlaneClient(TEST_ENDPOINT, TEST_AGENT, (v, e) -> {
    })) {
      setAtomicReferenceValue(sut, "streamFailure", new RuntimeException("boom"));
      CountDownLatch latch = (CountDownLatch) getFieldValue(sut, "shutdownSignal");
      latch.countDown();

      assertThatThrownBy(sut::await)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Control-plane stream failed.");
    }
  }

  @Test
  void closeShouldBeSafeWhenClientWasNotStarted() {
    AgentControlPlaneClient sut = new AgentControlPlaneClient(TEST_ENDPOINT, TEST_AGENT, (v, e) -> {
    });
    assertThatCode(sut::close).doesNotThrowAnyException();
  }

  @Test
  void inboundObserverShouldApplyConfigAndSendPong() throws Exception {
    AtomicReference<String> versionRef = new AtomicReference<>();
    AtomicReference<Map<String, String>> entriesRef = new AtomicReference<>();
    try (AgentControlPlaneClient sut =
        new AgentControlPlaneClient(TEST_ENDPOINT, TEST_AGENT, (version, entries) -> {
          versionRef.set(version);
          entriesRef.set(entries);
        })) {
      RecordingAgentObserver requestObserver = new RecordingAgentObserver();
      setAtomicReferenceValue(sut, "requestObserver", requestObserver);

      Object inboundObserver = newInboundObserver(sut);
      Method onNext = inboundObserver.getClass().getMethod("onNext", ControlPlaneEvent.class);

      onNext.invoke(
          inboundObserver,
          ControlPlaneEvent.newBuilder()
              .setPing(
                  ControlPlanePing.newBuilder()
                      .setPingId(INBOUND_PING_ID)
                      .setSentAtEpochMs(INBOUND_SENT_AT_EPOCH_MS)
                      .build())
              .build());
      onNext.invoke(
          inboundObserver,
          ControlPlaneEvent.newBuilder()
              .setConfigUpdate(GrpcEventDataFactory.createConfigUpdateFromResource("testdata/grpc/config-update.json"))
              .build());

      assertThat(requestObserver.values).hasSize(EXPECTED_SINGLE_EVENT);
      assertThat(requestObserver.values.get(0).hasPong()).isTrue();
      assertThat(requestObserver.values.get(0).getPong().getPingId()).isEqualTo(INBOUND_PING_ID);
      assertThat(requestObserver.values.get(0).getPong().getAgentName()).isEqualTo(TEST_AGENT);
      assertThat(versionRef.get()).isEqualTo(EXPECTED_CONFIG_VERSION);
      assertThat(entriesRef.get()).containsEntry("k", "v");
    }
  }

  @Test
  void inboundObserverOnCompletedShouldReleaseAwait() throws Exception {
    try (AgentControlPlaneClient sut = new AgentControlPlaneClient(TEST_ENDPOINT, TEST_AGENT, (v, e) -> {
    })) {
      Object inboundObserver = newInboundObserver(sut);
      Method onCompleted = inboundObserver.getClass().getMethod("onCompleted");
      onCompleted.invoke(inboundObserver);

      assertThatCode(sut::await).doesNotThrowAnyException();
    }
  }

  private static Object newInboundObserver(AgentControlPlaneClient client) throws Exception {
    Class<?> inboundType =
        Class.forName("io.github.architrace.grpc.AgentControlPlaneClient$ControlPlaneInboundObserver");
    Constructor<?> constructor = inboundType.getDeclaredConstructor(AgentControlPlaneClient.class);
    constructor.setAccessible(true);
    return constructor.newInstance(client);
  }

  private static Object getFieldValue(Object target, String name) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(target);
  }

  @SuppressWarnings("unchecked")
  private static void setAtomicReferenceValue(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    ((AtomicReference<Object>) field.get(target)).set(value);
  }

  private static final class RecordingAgentObserver implements StreamObserver<AgentEvent> {
    private final List<AgentEvent> values = new ArrayList<>();

    @Override
    public void onNext(AgentEvent value) {
      values.add(value);
    }

    @Override
    public void onError(Throwable throwable) {
      // No-op for tests; this observer only records outbound events.
    }

    @Override
    public void onCompleted() {
      // No-op for tests; completion callback is irrelevant for assertions.
    }
  }
}
