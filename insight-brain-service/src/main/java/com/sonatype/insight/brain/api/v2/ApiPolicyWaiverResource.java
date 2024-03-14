/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRequestPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.api.v2.dto.sourcecontrol.ApiComponentPolicyWaiversDTO;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.webhook.RequestPolicyWaiverEventService;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.90
 */
@Named
@Timed
@Path(PublicApiPaths.POLICY_WAIVER_PATH)
public class ApiPolicyWaiverResource
{
  private final ApiPolicyWaiverService apiPolicyWaiverService;

  private final RequestPolicyWaiverEventService requestPolicyWaiverEventService;

  static final String OWNERS_PATH =
      "{ownerType: application|organization|repository|repository_manager|repository_container}/{ownerId}";

  static final String BY_POLICY_WAIVER_ID_PATH = OWNERS_PATH + "/{policyWaiverId}";

  static final String BY_POLICY_VIOLATION_ID_PATH = OWNERS_PATH + "/{policyViolationId}";

  static final String TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH = "transitive/{ownerType: application}/{ownerId}/{scanId}";

  static final String TRANSITIVE_VIOLATIONS_BY_STAGE_ID_PATH =
      "transitive/{ownerType: application|organization}/{ownerId}/stages/{stageId}";

  static final String REQUEST_WAIVER_BY_POLICY_VIOLATION_ID_PATH = "/waiverRequests/{policyViolationId}";

  @Inject
  public ApiPolicyWaiverResource(
      ApiPolicyWaiverService apiPolicyWaiverService,
      RequestPolicyWaiverEventService requestPolicyWaiverEventService)
  {
    this.apiPolicyWaiverService = apiPolicyWaiverService;
    this.requestPolicyWaiverEventService = requestPolicyWaiverEventService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_WAIVER)
  @Path(BY_POLICY_VIOLATION_ID_PATH)
  public void addPolicyWaiverByPolicyViolationId(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @PathParam("policyViolationId") String policyViolationId,
      ApiWaiverOptionsDTO waiverOptionsDTO)
  {
    apiPolicyWaiverService.addPolicyWaiverByPolicyViolationId(ownerType, ownerId, policyViolationId, waiverOptionsDTO);
  }

  /**
   * @since 1.147
   */
  @GET
  @Audited(AuditEvent.VIEW_WAIVER)
  @Path(BY_POLICY_WAIVER_ID_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiPolicyWaiverDTO getPolicyWaiver(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @PathParam("policyWaiverId") String policyWaiverId)
  {
    return apiPolicyWaiverService.getPolicyWaiver(ownerType, ownerId, policyWaiverId);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_WAIVER)
  @Path(BY_POLICY_WAIVER_ID_PATH)
  public void deletePolicyWaiver(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @PathParam("policyWaiverId") String policyWaiverId)
  {
    apiPolicyWaiverService.deletePolicyWaiver(ownerType, ownerId, policyWaiverId);
  }

  @GET
  @Path(OWNERS_PATH)
  @Audited(AuditEvent.VIEW_WAIVER)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiPolicyWaiverDTO> getPolicyWaivers(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId)
  {
    return apiPolicyWaiverService.getPolicyWaivers(ownerType, ownerId);
  }

  @POST
  @Path(TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH)
  @Audited(AuditEvent.CREATE_TRANSITIVE_POLICY_VIOLATIONS_WAIVER)
  @Consumes(MediaType.APPLICATION_JSON)
  public void addWaiverToTransitivePolicyViolationsByAppScanComponent(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      @PathParam("scanId") final String scanId,
      @QueryParam("componentIdentifier") final ComponentIdentifier componentIdentifier,
      @QueryParam("packageUrl") final String packageUrl,
      @QueryParam("hash") final String hash,
      ApiWaiverOptionsDTO apiWaiverOptionsDTO)
  {
    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByAppScanComponent(ownerType, ownerId, scanId,
        componentIdentifier, packageUrl, hash, apiWaiverOptionsDTO);
  }

  @POST
  @Path(TRANSITIVE_VIOLATIONS_BY_STAGE_ID_PATH)
  @Audited(AuditEvent.CREATE_TRANSITIVE_POLICY_VIOLATIONS_WAIVER)
  @Consumes(MediaType.APPLICATION_JSON)
  public void addWaiverToTransitivePolicyViolationsByOwnerStageComponent(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      @PathParam("stageId") final String stageId,
      @QueryParam("componentIdentifier") final ComponentIdentifier componentIdentifier,
      @QueryParam("packageUrl") final String packageUrl,
      @QueryParam("hash") final String hash,
      ApiWaiverOptionsDTO apiWaiverOptionsDTO)
  {
    apiPolicyWaiverService.addWaiverToTransitivePolicyViolationsByOwnerStageComponent(ownerType, ownerId, stageId,
        componentIdentifier, packageUrl, hash, apiWaiverOptionsDTO);
  }

  @GET
  @Path(TRANSITIVE_VIOLATIONS_BY_SCAN_ID_PATH)
  @Audited(AuditEvent.VIEW_WAIVER)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiComponentPolicyWaiversDTO getTransitivePolicyWaiversByAppScanComponent(
      @PathParam("ownerType") final OwnerType ownerType,
      @PathParam("ownerId") final String ownerId,
      @PathParam("scanId") final String scanId,
      @QueryParam("componentIdentifier") final ComponentIdentifier componentIdentifier,
      @QueryParam("packageUrl") final String packageUrl,
      @QueryParam("hash") final String hash)
  {
    return apiPolicyWaiverService.getTransitivePolicyWaiversByAppScanComponent(ownerType, ownerId, scanId,
        componentIdentifier, packageUrl, hash);
  }

  /**
   * @since 1.164
   */
  @POST
  @Path(REQUEST_WAIVER_BY_POLICY_VIOLATION_ID_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  public void requestPolicyWaiver(
      @PathParam("policyViolationId") final String policyViolationId,
      ApiRequestPolicyWaiverDTO requestWaiverDTO)
  {
    requestPolicyWaiverEventService.postRequestPolicyWaiverEvent(policyViolationId, requestWaiverDTO);
  }
}
