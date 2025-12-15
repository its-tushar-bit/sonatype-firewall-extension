/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

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
  public Response createSupportZip(@QueryParam("includeDb") final boolean includeDb,
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
