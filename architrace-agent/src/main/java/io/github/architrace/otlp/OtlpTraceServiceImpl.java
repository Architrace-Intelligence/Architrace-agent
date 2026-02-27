/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtlpTraceServiceImpl extends TraceServiceGrpc.TraceServiceImplBase {

  private static final Logger log = LoggerFactory.getLogger(OtlpTraceServiceImpl.class);

  @Override
  public void export(
      ExportTraceServiceRequest request,
      StreamObserver<ExportTraceServiceResponse> responseObserver) {
    long spanCount = 0L;
    for (var resourceSpans : request.getResourceSpansList()) {
      for (var scopeSpans : resourceSpans.getScopeSpansList()) {
        for (var span : scopeSpans.getSpansList()) {
          log.info("span {}", span);
          spanCount++;
        }
      }
    }

    log.info("OTLP export received on receiver: {} span(s).", spanCount);
    responseObserver.onNext(ExportTraceServiceResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
