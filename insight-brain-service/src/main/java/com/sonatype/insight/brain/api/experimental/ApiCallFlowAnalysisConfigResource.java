/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.callflowanalysis.ApiCallFlowAnalysisConfigDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;

import static com.sonatype.insight.brain.api.PublicApiPaths.CALL_FLOW_ANALYSIS_CONFIG;

@Named
@Timed
@Path(CALL_FLOW_ANALYSIS_CONFIG)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(LicensedFeature.CALL_FLOW_ANALYSIS)
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
