/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.controlplane;

import io.github.architrace.grpc.proto.ConfigUpdate;
import io.github.architrace.grpc.proto.ControlPlanePing;
import io.github.architrace.grpc.proto.ControlPlaneEvent;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ControlPlaneRegistry {

  private final ConcurrentHashMap<String, AgentSession> sessions = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicLong> versionSequence = new ConcurrentHashMap<>();
  private final AtomicLong pingSequence = new AtomicLong(0);

  public void register(String agentName, StreamObserver<ControlPlaneEvent> responseObserver) {
    sessions.put(
        agentName,
        new AgentSession(
            responseObserver,
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            new AtomicLong(0)));

    sendConfig(
        agentName,
        ConfigUpdate.newBuilder()
            .setVersion(nextVersion(agentName))
            .putEntries("agent.mode", "managed")
            .putEntries("agent.bootstrap", "done")
            .build(),
        responseObserver);
  }

  public void pong(String agentName, long pingId, long sentAtEpochMs) {
    AgentSession session = sessions.get(agentName);
    if (session != null) {
      session.lastPongId.set(pingId);
      session.lastSeenEpochMs = Math.max(sentAtEpochMs, System.currentTimeMillis());
    }
  }

  public void unregister(String agentName) {
    if (agentName != null) {
      sessions.remove(agentName);
    }
  }

  public HealthState health(String agentName, long liveThresholdMs) {
    AgentSession session = sessions.get(agentName);
    if (session == null) {
      return new HealthState(false, 0L);
    }

    long lastSeen = session.lastSeenEpochMs;
    boolean live = (System.currentTimeMillis() - lastSeen) <= liveThresholdMs;
    return new HealthState(live, lastSeen);
  }

  public void tick() {
    long pingId = pingSequence.incrementAndGet();
    long now = System.currentTimeMillis();
    for (Map.Entry<String, AgentSession> entry : sessions.entrySet()) {
      String agentName = entry.getKey();
      AgentSession session = entry.getValue();

      session.responseObserver.onNext(
          ControlPlaneEvent.newBuilder()
              .setPing(
                  ControlPlanePing.newBuilder()
                      .setPingId(pingId)
                      .setSentAtEpochMs(now)
                      .build())
              .build());

      if ((pingId % 3) == 0) {
        sendConfig(
            agentName,
            ConfigUpdate.newBuilder()
                .setVersion(nextVersion(agentName))
                .putEntries("control.lastPingId", Long.toString(pingId))
                .putEntries("control.updatedAtEpochMs", Long.toString(now))
                .build(),
            session.responseObserver);
      }
    }
  }

  private String nextVersion(String agentName) {
    AtomicLong seq = versionSequence.computeIfAbsent(agentName, ignored -> new AtomicLong(0));
    return Long.toString(seq.incrementAndGet());
  }

  private void sendConfig(
      String agentName, ConfigUpdate update, StreamObserver<ControlPlaneEvent> responseObserver) {
    responseObserver.onNext(ControlPlaneEvent.newBuilder().setConfigUpdate(update).build());
  }

  public record HealthState(boolean live, long lastSeenEpochMs) {
  }

  private static final class AgentSession {
    private final StreamObserver<ControlPlaneEvent> responseObserver;
    private volatile long lastSeenEpochMs;
    private volatile long connectedAtEpochMs;
    private final AtomicLong lastPongId;

    private AgentSession(
        StreamObserver<ControlPlaneEvent> responseObserver,
        long lastSeenEpochMs,
        long connectedAtEpochMs,
        AtomicLong lastPongId) {
      this.responseObserver = responseObserver;
      this.lastSeenEpochMs = lastSeenEpochMs;
      this.connectedAtEpochMs = connectedAtEpochMs;
      this.lastPongId = lastPongId;
    }
  }
}
