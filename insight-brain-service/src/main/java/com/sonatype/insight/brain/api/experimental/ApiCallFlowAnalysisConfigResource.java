/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.callflowanalysis.ApiCallFlowAnalysisConfigDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;

import com.codahale.metrics.annotation.Timed;

import static com.sonatype.insight.brain.api.PublicApiPaths.CALL_FLOW_ANALYSIS_CONFIG;

@Named
@Timed
@Path(CALL_FLOW_ANALYSIS_CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ApiCallFlowAnalysisConfigResource
{
  private final ApiCallFlowAnalysisConfigService apiCallFlowAnalysisService;

  @Inject
  public ApiCallFlowAnalysisConfigResource(ApiCallFlowAnalysisConfigService apiCallFlowAnalysisService) {
    this.apiCallFlowAnalysisService = apiCallFlowAnalysisService;
  }

  @GET
  public ApiCallFlowAnalysisConfigDTO getCallFlowAnalysisConfig(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId)
  {
    return apiCallFlowAnalysisService.getCallFlowAnalysisConfig(ownerType, ownerId);
  }

  @PUT
  @Audited(AuditEvent.CONFIGURE_CALL_FLOW_ANALYSIS)
  public ApiCallFlowAnalysisConfigDTO upsertCallFlowAnalysisConfig(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      final ApiCallFlowAnalysisConfigDTO callFlowAnalysisConfig)
  {
    return apiCallFlowAnalysisService.upsertCallFlowAnalysisConfig(ownerType, ownerId, callFlowAnalysisConfig);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_CONFIGURE_CALL_FLOW_ANALYSIS)
  public void deleteCallFlowAnalysisConfig(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId)
  {
    apiCallFlowAnalysisService.deleteCallFlowAnalysisConfig(ownerType, ownerId);
  }

  @GET
  @Path("/publicId")
  public ApiCallFlowAnalysisConfigDTO getCallFlowAnalysisConfigByPublicId(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId)
  {
    return apiCallFlowAnalysisService.getCallFlowAnalysisConfigByPublicId(ownerType, ownerId);
  }
}
