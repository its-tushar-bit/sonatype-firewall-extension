/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.hds.ComponentInfoService.ComponentLicenses;
import com.sonatype.insight.jaxrs.JsonEncodedComponentIdentifier;

@Path(CIComponentInfoResource.RESOURCE_PATH)
@Named
public class CIComponentInfoResource
{
  public static final String RESOURCE_PATH = "rest/ci/componentDetails";

  private final ComponentInfoService componentInfoService;

  @Context
  private HttpServletRequest httpRequest;

  @Inject
  public CIComponentInfoResource(ComponentInfoService componentInfoService) {
    this.componentInfoService = componentInfoService;
    componentInfoService.setToolName("ci");
  }

  @GET
  @Path("{applicationPublicId}")
  @Produces(MediaType.APPLICATION_JSON)
  public NamedComponentDetails getComponentDetails(@PathParam("applicationPublicId") String applicationPublicId,
      @QueryParam("reportId") String reportId,
      @QueryParam("componentIdentifier") JsonEncodedComponentIdentifier identifier,
      @QueryParam("matchState") String matchState, @QueryParam("hash") String hash,
      @QueryParam("proprietary") boolean proprietary) throws IOException
  {
    return componentInfoService.getComponentDetails_ReadPermission(applicationPublicId, reportId, identifier,
        matchState, hash, proprietary, httpRequest);
  }

  @GET
  @Path("{applicationPublicId}/list")
  @Produces(MediaType.APPLICATION_JSON)
  public ComponentDetailsList getComponentDetailsList(@PathParam("applicationPublicId") String applicationPublicId,
      @QueryParam("reportId") String reportId,
      @QueryParam("componentIdentifier") JsonEncodedComponentIdentifier identifier,
      @QueryParam("matchState") String matchState) throws IOException
  {
    return componentInfoService.getComponentDetailsList_ReadPermission(applicationPublicId, reportId, identifier,
        matchState, httpRequest);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("licenses/{applicationPublicId}")
  public ComponentLicenses getLicenses(@PathParam("applicationPublicId") String applicationPublicId,
      @QueryParam("componentIdentifier") JsonEncodedComponentIdentifier componentIdentifier) throws IOException
  {
    return componentInfoService.getLicenses(applicationPublicId, componentIdentifier, httpRequest);
  }
}
