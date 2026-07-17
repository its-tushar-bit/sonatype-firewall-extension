/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentVersionInfoDTO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

import static com.sonatype.insight.brain.telemetry.NonBreakingRecommendationTelemetryStats.SourceEndpoint.IDE;

@Path(IDEComponentInfoResource.RESOURCE_PATH)
@Named
@Timed
@ProductLicenseEnforcementPoint(LicensedFeature.COMPONENT_EVALUATION)
public class IDEComponentInfoResource
{
  public static final String RESOURCE_PATH = "rest/ide/componentDetails";

  static final String APPLICATION_COMPONENT_DETAILS_PATH = "application/{applicationPublicId}";

  private final ComponentInfoService componentInfoService;

  @Inject
  public IDEComponentInfoResource(
      ComponentInfoService componentInfoService,
      @Named("ideComponentDetails") HdsClient ideHdsClient)
  {
    this.componentInfoService = componentInfoService;
    this.componentInfoService.setHdsClient(ideHdsClient);
    this.componentInfoService.setToolName("ide");
  }

  @GET
  @Path(APPLICATION_COMPONENT_DETAILS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public NamedComponentDetails getComponentDetails(
      @PathParam("applicationPublicId") String applicationPublicId,
      @QueryParam("componentIdentifier") ComponentIdentifier identifier,
      @QueryParam("matchState") String matchState,
      @QueryParam("hash") String hash,
      @QueryParam("proprietary") boolean proprietary,
      @Context HttpServletRequest httpRequest) throws IOException
  {
    return componentInfoService.getComponentDetails_EvaluateComponentPermission(applicationPublicId, identifier,
        matchState, hash, proprietary, httpRequest);
  }

  /**
   * @deprecated since 1.48. Not used by Insight or plugins, but left here as our customers use these APIs.
   */
  @Deprecated
  @GET
  @Path(APPLICATION_COMPONENT_DETAILS_PATH + "/list")
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public ComponentDetailsList getComponentDetailsList(
      @PathParam("applicationPublicId") String applicationPublicId,
      @QueryParam("componentIdentifier") ComponentIdentifier identifier,
      @QueryParam("matchState") String matchState)
  {
    return componentInfoService.getComponentDetailsList_EvaluateComponentPermission(applicationPublicId, identifier,
        matchState);
  }

  /**
   * @since 1.48
   */
  @GET
  @Path(APPLICATION_COMPONENT_DETAILS_PATH + "/allVersions")
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENT_INFORMATION)
  public ComponentVersionInfoDTO getComponentVersionInfo(
      @PathParam("applicationPublicId") String applicationPublicId,
      @QueryParam("componentIdentifier") ComponentIdentifier componentIdentifier)
  {
    return componentInfoService.getComponentVersionInfo_EvaluateComponentPermission(applicationPublicId,
        componentIdentifier, IDE);
  }
}
