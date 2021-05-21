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

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentTransitivePolicyViolationsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiCrossStageViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiversApplicableToViolationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyViolationServiceV2;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.InsightConfig.Feature;

import com.codahale.metrics.annotation.Timed;
import org.apache.shiro.authz.UnauthorizedException;

/**
 * @since 1.13.0
 */
@Named
@Timed
@Path(PublicApiPaths.POLICY_VIOLATION_RESOURCE_PATH_V2)
public class DefaultApiPolicyViolationResourceV2 implements ApiPolicyViolationResourceV2
{
  public static final String CROSS_STAGE_POLICY_VIOLATION_SUBPATH = "crossStage";

  public static final String VIOLATIONID = "/{violationId}";

  public static final String APPLICABLE_WAIVERS_PATH = "/applicableWaivers";

  public static final String TRANSITIVE_VIOLATIONS_PATH =
      "transitive/{ownerType: application|organization}/{ownerId}/stages/{stageId}";

  private ApiPolicyViolationServiceV2 apiPolicyViolationService;

  private final ApiCrossStageViolationService apiCrossStageViolationService;

  private final ApiPolicyWaiverService apiPolicyWaiverService;

  @Inject
  public DefaultApiPolicyViolationResourceV2(
      final ApiPolicyViolationServiceV2 apiPolicyViolationService,
      final ApiCrossStageViolationService apiCrossStageViolationService,
      final ApiPolicyWaiverService apiPolicyWaiverService)
  {
    this.apiPolicyViolationService = apiPolicyViolationService;
    this.apiCrossStageViolationService = apiCrossStageViolationService;
    this.apiPolicyWaiverService = apiPolicyWaiverService;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EXPORT_POLICY_VIOLATIONS)
  public ApiApplicationViolationListDTOV2 getPolicyViolations(@QueryParam("p") final Set<String> policyIds) {
    return apiPolicyViolationService.getPolicyViolations(policyIds);
  }

  @Override
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

  @Override
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

  @Override
  @GET
  @Path(VIOLATIONID + APPLICABLE_WAIVERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENTS_WITH_WAIVERS)
  public ApiPolicyWaiversApplicableToViolationDTO getApplicableWaivers(
      @PathParam("violationId") final String violationId)
  {
    return apiPolicyWaiverService.getApplicableWaivers(violationId);
  }

  @Override
  @GET
  @Path(TRANSITIVE_VIOLATIONS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_COMPONENT_TRANSITIVE_POLICY_VIOLATIONS)
  public ApiComponentTransitivePolicyViolationsDTO getTransitivePolicyViolations(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      @PathParam("stageId") final String stageId,
      @QueryParam("componentIdentifier") final ComponentIdentifier componentIdentifier,
      @QueryParam("packageUrl") final String packageUrl,
      @QueryParam("hash") final String hash)
  {
    if (!apiPolicyViolationService.isInnerSourceTransitiveWaiverEnabled()) {
      throw new UnauthorizedException(Feature.INNER_SOURCE_TRANSITIVE_WAIVER.getFlag() + " feature is disabled.");
    }
    return apiPolicyViolationService
        .getTransitivePolicyViolations(ownerType, ownerId, stageId, componentIdentifier, packageUrl, hash);
  }
}
