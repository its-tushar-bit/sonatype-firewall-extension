/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.product.license.UnlicensedPath;
import com.sonatype.insight.brain.api.IqOnlyEndpoint;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;

import com.codahale.metrics.annotation.Timed;

import static com.sonatype.insight.brain.support.SupportResource.RESOURCE_PATH;

/**
 * To create a support zip file via curl:
 * curl -u admin:admin123 http://localhost:8070/rest/support?includeDb=true >support.zip
 *
 * @since 1.27
 */
@Named
@IqOnlyEndpoint
@Timed
@UnlicensedPath
@Path(RESOURCE_PATH)
public class SupportResource
{
  public static final String RESOURCE_PATH = "rest/support";

  private final SupportService supportService;

  @Inject
  public SupportResource(final SupportService supportService) {
    this.supportService = supportService;
  }

  @GET
  @Produces("application/zip")
  public Response createSupportZip(
      @QueryParam("includeDb") final boolean includeDb,
      @QueryParam("noLimit") final boolean noLimit,
      @Context final HttpServletRequest request) throws IOException
  {
    final String requestUrl = request.getRequestURL().toString();

    try {
      final File supportZip = supportService.createSupportZip(includeDb, requestUrl, noLimit);

      final ResponseBuilder response = Response.ok();
      response.entity(supportZip);
      response.header(HttpHeaders.CONTENT_DISPOSITION,
          HttpHeaderUtils.buildContentDispositionHeaderValue(supportZip.getName()));
      return response.build();
    }
    catch (SupportZipInProgressException e) {
      return Response.status(Status.TOO_MANY_REQUESTS)
          .entity(e.getMessage())
          .build();
    }
  }
}
