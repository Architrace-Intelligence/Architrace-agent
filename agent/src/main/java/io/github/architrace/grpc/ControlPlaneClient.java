/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.grpc;

import io.github.architrace.grpc.proto.AgentEvent;
import io.github.architrace.grpc.proto.AgentPong;
import io.github.architrace.grpc.proto.AgentRegister;
import io.github.architrace.grpc.proto.ControlPlaneEvent;
import io.github.architrace.grpc.proto.ControlPlaneServiceGrpc;
import io.github.architrace.runtime.AgentConfigApplier;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlPlaneClient implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(ControlPlaneClient.class);

  private final String agentName;
  private final AgentConfigApplier configApplier;
  private final ManagedChannel channel;
  private final CountDownLatch shutdownSignal = new CountDownLatch(1);

  private final AtomicReference<StreamObserver<AgentEvent>> requestObserver = new AtomicReference<>();
  private final AtomicReference<Throwable> streamFailure = new AtomicReference<>();

  public ControlPlaneClient(String server, String agentName, AgentConfigApplier configApplier) {
    this.agentName = agentName;
    this.configApplier = configApplier;
    var address = GrpcAddressParser.parseHostPort(server);
    this.channel = NettyChannelBuilder.forAddress(address).usePlaintext().build();
  }

  public void start() {
    ControlPlaneServiceGrpc.ControlPlaneServiceStub stub = ControlPlaneServiceGrpc.newStub(channel);
    StreamObserver<AgentEvent> observer = stub.connect(new ControlPlaneInboundObserver());
    requestObserver.set(observer);

    observer.onNext(
        AgentEvent.newBuilder()
            .setRegister(AgentRegister.newBuilder().setAgentName(agentName).build())
            .build());
  }

  public void await() throws InterruptedException {
    shutdownSignal.await();
    Throwable failure = streamFailure.get();
    if (failure != null) {
      throw new IllegalStateException("Control-plane stream failed.", failure);
    }
  }

  @Override
  public void close() {
    StreamObserver<AgentEvent> observer = requestObserver.get();
    if (observer != null) {
      observer.onCompleted();
    }

    channel.shutdownNow();
    try {
      channel.awaitTermination(2, TimeUnit.SECONDS);
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
    }
  }

  private final class ControlPlaneInboundObserver implements StreamObserver<ControlPlaneEvent> {
    private void sendPong(long pingId) {
      StreamObserver<AgentEvent> observer = requestObserver.get();
      if (observer == null) {
        return;
      }
      observer.onNext(
          AgentEvent.newBuilder()
              .setPong(
                  AgentPong.newBuilder()
                      .setAgentName(agentName)
                      .setPingId(pingId)
                      .setSentAtEpochMs(System.currentTimeMillis())
                      .build())
              .build());
    }

    @Override
    public void onNext(ControlPlaneEvent message) {
      log.info("Received control plane event {}", message);
      if (message.hasPing()) {
        sendPong(message.getPing().getPingId());
      }
      if (message.hasConfigUpdate()) {
        var update = message.getConfigUpdate();
        configApplier.apply(update.getVersion(), update.getEntriesMap());
      }
    }

    @Override
    public void onError(Throwable throwable) {
      streamFailure.set(throwable);
      log.error("Control-plane stream failed for agent '{}'", agentName, throwable);
      shutdownSignal.countDown();
    }

    @Override
    public void onCompleted() {
      log.info("Control-plane stream completed for agent '{}'", agentName);
      shutdownSignal.countDown();
    }
  }
}
