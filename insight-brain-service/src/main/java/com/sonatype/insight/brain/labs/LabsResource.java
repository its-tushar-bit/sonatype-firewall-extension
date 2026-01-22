/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.labs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.IqOnlyEndpoint;

import org.apache.commons.lang3.StringUtils;

@Named
@IqOnlyEndpoint
@Path(LabsResource.RESOURCE_PATH)
public class LabsResource
{
  public static final String RESOURCE_PATH = "rest/labs";

  private final LabsService labsService;

  @Inject
  public LabsResource(LabsService labsService) {
    this.labsService = labsService;
  }

  @POST
  public Response labsPostMethod(@Context final HttpServletRequest httpRequest) throws IOException {
    return labsService.getLabsResponse(httpRequest, null);
  }

  @POST
  @Path("/{var:.*}")
  public Response labsPostMethodExtended(@Context final HttpServletRequest httpRequest) throws IOException {
    return labsService.getLabsResponse(httpRequest, null);
  }

  @GET
  public Response labsGetMethod(
      @Context final HttpServletRequest httpRequest,
      @QueryParam("command") String command,
      @QueryParam("values") String values) throws IOException
  {
    Map<String, String> queryParams = getQueryParamsMap(command, values);

    return labsService.getLabsResponse(httpRequest, queryParams);
  }

  @GET
  @Path("/{var:.*}")
  public Response labsGetMethodExtended(
      @Context final HttpServletRequest httpRequest,
      @QueryParam("command") String command,
      @QueryParam("values") String values) throws IOException
  {
    Map<String, String> queryParams = getQueryParamsMap(command, values);

    return labsService.getLabsResponse(httpRequest, queryParams);
  }

  private Map<String, String> getQueryParamsMap(
      @QueryParam("command") final String command,
      @QueryParam("values") final String values)
  {
    Map<String, String> queryParams = null;
    if (StringUtils.isNotEmpty(command)) {
      queryParams = new HashMap<>();
      queryParams.put("command", command);
      queryParams.put("values", values);
    }
    return queryParams;
  }
}
