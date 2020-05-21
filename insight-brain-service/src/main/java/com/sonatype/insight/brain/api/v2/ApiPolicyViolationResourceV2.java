/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiCrossStageViolationDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyViolationServiceV2;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.13.0
 */
@Named
@Timed
@Path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
public class ApiPolicyViolationResourceV2
{
  public static final String CROSS_STAGE_POLICY_VIOLATION_SUBPATH = "crossStage";

  public static final String VIOLATIONID = "/{violationId}";

  private ApiPolicyViolationServiceV2 apiPolicyViolationService;

  private final ApiCrossStageViolationService apiCrossStageViolationService;

  @Inject
  public ApiPolicyViolationResourceV2(
      final ApiPolicyViolationServiceV2 apiPolicyViolationService,
      final ApiCrossStageViolationService apiCrossStageViolationService)
  {
    this.apiPolicyViolationService = apiPolicyViolationService;
    this.apiCrossStageViolationService = apiCrossStageViolationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_POLICY_VIOLATIONS)
  public ApiApplicationViolationListDTOV2 getPolicyViolations(@QueryParam("p") final Set<String> policyIds) {
    return apiPolicyViolationService.getPolicyViolations(policyIds);
  }

  /**
   * @since 1.86.0
   */
  @GET
  @Path(CROSS_STAGE_POLICY_VIOLATION_SUBPATH + VIOLATIONID)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_POLICY_VIOLATIONS)
  public ApiCrossStageViolationDTOV2 getCrossStagePolicyViolationById(
      @PathParam("violationId") final String violationId)
  {
    return apiCrossStageViolationService.getCrossStageViolationById(violationId);
  }

  @GET
  @Path(CROSS_STAGE_POLICY_VIOLATION_SUBPATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_POLICY_VIOLATIONS)
  public ApiCrossStageViolationDTOV2 getCrossStagePolicyViolationByConstituentId(
      @QueryParam("constituentId") final String constituentId)
  {
    return apiCrossStageViolationService.getCrossStageViolationByConstituentId(constituentId);
  }
}
