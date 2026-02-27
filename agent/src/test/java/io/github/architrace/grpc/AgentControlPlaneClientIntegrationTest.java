///*
// * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
// * SPDX-License-Identifier: Apache-2.0
// */
//package io.github.architrace.grpc;
//
//
//import io.github.architrace.grpc.proto.AgentHealthRequest;
//import io.github.architrace.grpc.proto.ControlPlaneServiceGrpc;
//import io.github.architrace.testsupport.TestDataProvider;
//import io.grpc.ManagedChannel;
//import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//import java.util.Map;
//import org.junit.jupiter.api.Test;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//class AgentControlPlaneClientIntegrationTest {
//
//  private static final String LOOPBACK_HOST = "127.0.0.1";
//  private static final String AGENT_NAME = "agent-a";
//  private static final int INITIAL_CONFIG_LATCH_COUNT = 1;
//  private static final long CONFIG_WAIT_TIMEOUT_SECONDS = 5L;
//  private static final long HEALTH_DEADLINE_SECONDS = 3L;
//  private static final long CHANNEL_SHUTDOWN_TIMEOUT_SECONDS = 2L;
//  private static final String INITIAL_CONFIG_VERSION = "1";
//
//  @Test
//  void clientShouldRegisterReceiveConfigAndRespondToHealthChecks() throws Exception {
//    int port = TestDataProvider.findFreePort();
//    String endpoint = LOOPBACK_HOST + ":" + port;
//
//    CountDownLatch configApplied = new CountDownLatch(INITIAL_CONFIG_LATCH_COUNT);
//    AtomicReference<String> versionRef = new AtomicReference<>();
//    AtomicReference<Map<String, String>> entriesRef = new AtomicReference<>();
//
//    try (ControlPlaneGrpcServer server = new ControlPlaneGrpcServer(port)) {
//      server.start();
//
//      try (ControlPlaneClient sut =
//          new ControlPlaneClient(
//              endpoint,
//              AGENT_NAME,
//              (version, entries) -> {
//                versionRef.set(version);
//                entriesRef.set(entries);
//                configApplied.countDown();
//              })) {
//        sut.start();
//
//        assertThat(configApplied.await(CONFIG_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
//        assertThat(versionRef.get()).isEqualTo(INITIAL_CONFIG_VERSION);
//        assertThat(entriesRef.get()).containsEntry("agent.mode", "managed");
//        assertThat(entriesRef.get()).containsEntry("agent.bootstrap", "done");
//
//        ManagedChannel healthChannel =
//            NettyChannelBuilder.forAddress(LOOPBACK_HOST, port).usePlaintext().build();
//        try {
//          var stub =
//              ControlPlaneServiceGrpc.newBlockingStub(healthChannel).withDeadlineAfter(HEALTH_DEADLINE_SECONDS, TimeUnit.SECONDS);
//          var health =
//              stub.getAgentHealth(AgentHealthRequest.newBuilder().setAgentName(AGENT_NAME).build());
//          assertThat(health.getLive()).isTrue();
//          assertThat(health.getLastSeenEpochMs()).isPositive();
//        } finally {
//          healthChannel.shutdownNow();
//          healthChannel.awaitTermination(CHANNEL_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
//        }
//      }
//    }
//  }
//}
