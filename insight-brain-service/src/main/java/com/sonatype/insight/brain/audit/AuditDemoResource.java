/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.jaxrs.JsonEncodedComponentIdentifier;

@Named
@Path("rest/audit")
public class AuditDemoResource
{
  private final ExecutorService executor = new ThreadPoolExecutor(0, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<>());

  /**
   * Some operation that is not (yet) audited.
   */
  @GET
  @Path("unaudited")
  @Produces(MediaType.TEXT_PLAIN)
  public String unauditedOperation() {
    return "OK";
  }

  /**
   * Your everyday synchronous operation.
   */
  @GET
  @Path("simple")
  @Produces(MediaType.TEXT_PLAIN)
  @Audited(AuditEvent.CREATE_APPLICATION)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public String simpleOperation(@QueryParam("fail") boolean fail,
                                @QueryParam("failable") JsonEncodedComponentIdentifier failable)
  {
    AuditData.get().addData("msg", "Hello World");
    if (fail) {
      throw new BadRequestException("Failed as instructed");
    }
    return Response.status(Status.OK).toString();
  }

  /**
   * Operation that finishes asynchronously.
   */
  @GET
  @Path("async")
  @Produces(MediaType.TEXT_PLAIN)
  @Audited(AuditEvent.EVALUATE_APPLICATION)
  public String asyncOperation(@QueryParam("fail") boolean fail) {
    Callable<String> asyncTask = () -> {
      AuditData.get().addData("msg", "Hello World");
      Thread.sleep(3 * 1000);
      if (fail) {
        throw new IOException("Failed as instructed");
      }
      System.out.println("TASK DONE");
      return "OK";
    };
    AuditData.get().continueAsync(asyncTask, executor::submit);
    return "Scheduled";
  }

  /**
   * Compound operation with sub operations that should produce separate audit records. The outcome of the sub
   * operations is independent from that of the outer operation.
   */
  @GET
  @Path("compound/independent")
  @Produces(MediaType.TEXT_PLAIN)
  @Audited(AuditEvent.DELETE_ORGANIZATION)
  public String compoundOperationWithIndependentSubOperations(@QueryParam("fail") boolean fail) {
    AuditData.get().addData("msg", "Hello World");
    for (int i = 0; i < 3; i++) {
      try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.DELETE_APPLICATION, true)) {
        subOperation(i);
      }
    }
    if (fail) {
      throw new BadRequestException("Failed as instructed");
    }
    return "OK";
  }

  /**
   * Compound operation with sub operations that should produce separate audit records. The outcome of the sub
   * operations depends on the outer operation, i.e. they only succeed if the outer operation does, say because they
   * belong to the same database transaction.
   */
  @GET
  @Path("compound/dependent")
  @Produces(MediaType.TEXT_PLAIN)
  @Audited(AuditEvent.DELETE_ORGANIZATION)
  public String compoundOperationWithDependentSubOperations(@QueryParam("fail") boolean fail) {
    AuditData.get().addData("msg", "Hello World");
    for (int i = 0; i < 3; i++) {
      try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.DELETE_APPLICATION, false)) {
        subOperation(i);
      }
    }
    if (fail) {
      throw new BadRequestException("Failed as instructed");
    }
    return "OK";
  }

  private void subOperation(int index) {
    AuditData.get().addData("index", index);
  }
}
