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
    long spanCount =
        request.getResourceSpansList().stream()
            .flatMap(resourceSpans -> resourceSpans.getScopeSpansList().stream())
            .flatMap(scopeSpans -> scopeSpans.getSpansList().stream())
            .peek(span -> log.info("span {}", span))
            .count();

    log.info("OTLP export received on receiver: {} span(s).", spanCount);
    responseObserver.onNext(ExportTraceServiceResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
