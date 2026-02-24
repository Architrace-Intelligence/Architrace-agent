/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class OtlpTraceReceiverServerIntegrationTest {

  @Test
  void shouldReceiveAndLogOtlpTraceExport() throws Exception {
    int port = findFreePort();

    Logger logger = (Logger) LoggerFactory.getLogger(OtlpTraceServiceImpl.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    try (OtlpTraceReceiverServer server = new OtlpTraceReceiverServer(port)) {
      server.start();

      ManagedChannel channel =
          NettyChannelBuilder.forAddress("127.0.0.1", port).usePlaintext().build();
      try {
        var stub = TraceServiceGrpc.newBlockingStub(channel).withDeadlineAfter(5, TimeUnit.SECONDS);
        var response = stub.export(buildRequestWithSingleSpan());
        assertNotNull(response);
      } finally {
        channel.shutdownNow();
        channel.awaitTermination(2, TimeUnit.SECONDS);
      }

      boolean logged =
          listAppender.list.stream()
              .map(ILoggingEvent::getFormattedMessage)
              .anyMatch(msg -> msg.contains("OTLP export received on receiver: 1 span(s)."));

      assertTrue(logged, "Expected receiver log confirming OTLP span ingestion.");
    } finally {
      logger.detachAppender(listAppender);
    }
  }

  private static ExportTraceServiceRequest buildRequestWithSingleSpan() {
    Span span =
        Span.newBuilder()
            .setTraceId(ByteString.copyFrom(new byte[16]))
            .setSpanId(ByteString.copyFrom(new byte[8]))
            .setName("integration-test-span")
            .build();

    ScopeSpans scopeSpans = ScopeSpans.newBuilder().addSpans(span).build();
    ResourceSpans resourceSpans = ResourceSpans.newBuilder().addScopeSpans(scopeSpans).build();

    return ExportTraceServiceRequest.newBuilder().addResourceSpans(resourceSpans).build();
  }

  private static int findFreePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
