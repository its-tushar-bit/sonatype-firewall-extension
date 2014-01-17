/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.error.ErrorResponse;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Accepts uploads of application binaries for the purpose of scanning them.
 *
 * @since 1.8
 */
@Named
@Path(ScanResource.SERVICE_PATH)
public class ScanResource
{
  public static final String SERVICE_PATH = "rest/scan/{applicationPublicId}";

  private final ScanService scanService;

  private final ErrorResponseGenerator errorResponseGenerator;

  @Inject
  public ScanResource(ScanService scanService, ErrorResponseGenerator errorResponseGenerator) {
    this.scanService = scanService;
    this.errorResponseGenerator = errorResponseGenerator;
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadBinary(
      @PathParam("applicationPublicId") String appPublicId,
      @FormDataParam("file") InputStream is, 
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @QueryParam("stageId") String stageId,
      @QueryParam("sendNotifications") boolean sendNotifications,
      @QueryParam("noFormData") boolean noFormData) 
          throws Exception
  {
    try {
      ScanTicket result = scanService.scanBinary(appPublicId, is, fileDetail.getFileName(), new Stage(stageId),
          sendNotifications);
      if (noFormData) {
        return Response.ok(new ObjectMapper().writeValueAsString(result), ErrorResponse.CONTENT_TYPE).build();
      } else {
        return Response.ok(result, MediaType.APPLICATION_JSON).build();  
      }
    }
    catch (Exception e) {
      if (noFormData) {
        String msg = errorResponseGenerator.mapException(e).getMessageBody();
        return Response.ok(msg, ErrorResponse.CONTENT_TYPE).build();
      }
      throw e;
    }
  }

  @Path("/{ticketId}")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ScanTicket getTicket(
      @PathParam("applicationPublicId") String appPublicId,
      @PathParam("ticketId") String ticketId) throws Exception
  {
    return scanService.getTicket(appPublicId, ticketId);
  }
}
