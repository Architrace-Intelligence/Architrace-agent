/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.service.grpc;

import io.github.architrace.grpc.proto.AgentEvent;
import io.github.architrace.grpc.proto.AgentHealthRequest;
import io.github.architrace.grpc.proto.AgentHealthResponse;
import io.github.architrace.grpc.proto.ControlPlaneEvent;
import io.github.architrace.grpc.proto.ControlPlanePing;
import io.github.architrace.grpc.proto.ControlPlaneServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class AgentService extends ControlPlaneServiceGrpc.ControlPlaneServiceImplBase {

  @Override
  public StreamObserver<AgentEvent> connect(StreamObserver<ControlPlaneEvent> responseObserver) {
    return new StreamObserver<AgentEvent>() {

      @Override
      public void onNext(AgentEvent agentEvent) {
        System.out.println("Получено событие от агента: " + agentEvent.getRegister().getAgentName());
        System.out.println("Получено событие от агента: " + agentEvent.getPong().getAgentName());


        ControlPlaneEvent response = ControlPlaneEvent.newBuilder()
            .setPing(ControlPlanePing.newBuilder().setPingId(100).build())
            .build();
        responseObserver.onNext(response);
      }

      @Override
      public void onError(Throwable throwable) {
        System.err.println("Ошибка от агента: " + throwable.getMessage());
      }

      @Override
      public void onCompleted() {
        responseObserver.onCompleted();
      }
    };
  }

  @Override
  public void getAgentHealth(AgentHealthRequest request,
                             StreamObserver<AgentHealthResponse> responseObserver) {
    super.getAgentHealth(request, responseObserver);
  }
}
