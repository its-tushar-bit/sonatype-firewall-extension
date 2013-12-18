/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.service.ErrorResponseGenerator;
import com.sonatype.insight.error.ErrorResponse;

import com.sun.jersey.multipart.FormDataParam;

/**
 * Accepts uploads of application binaries for the purpose of scanning them.
 * 
 * @since 1.7.1
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
  public Response uploadBinary(@PathParam("applicationPublicId") String appPublicId,
      @FormDataParam("file") InputStream is, @QueryParam("forceSuccess") boolean forceSuccess) throws Exception
  {
    try {
      ScanTicket result = scanService.scanBinary(appPublicId, is);
      return Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build();
    }
    catch (Exception e) {
      if (forceSuccess) {
        String msg = errorResponseGenerator.mapException(e).getMessageBody();
        return Response.ok(msg, ErrorResponse.CONTENT_TYPE).build();
      }
      throw e;
    }
  }
}
