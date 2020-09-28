/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;
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
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.api.v2.service.ApiPolicyWaiverService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.90
 */
@Named
@Timed
@Path(PublicApiPaths.POLICY_WAIVER_PATH)
public class ApiPolicyWaiverResource
{
  private ApiPolicyWaiverService apiPolicyWaiverService;

  static final String BY_POLICY_WAIVER_ID_PATH = "{policyWaiverId}";

  static final String BY_POLICY_VIOLATION_ID_PATH = "{policyViolationId}";

  @Inject
  public ApiPolicyWaiverResource(ApiPolicyWaiverService apiPolicyWaiverService) {
    this.apiPolicyWaiverService = apiPolicyWaiverService;
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
    String comment = waiverOptionsDTO == null ? null : waiverOptionsDTO.comment;
    boolean applyToAllComponents = waiverOptionsDTO != null && waiverOptionsDTO.applyToAllComponents;
    Date expiryTime = waiverOptionsDTO == null ? null : waiverOptionsDTO.expiryTime;

    apiPolicyWaiverService
        .addPolicyWaiverByPolicyViolationId(
            ownerType,
            ownerId,
            policyViolationId,
            comment,
            applyToAllComponents,
            expiryTime);
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
  @Audited(AuditEvent.VIEW_WAIVER)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiPolicyWaiverDTO> getPolicyWaivers(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId)
  {
    return apiPolicyWaiverService.getPolicyWaivers(ownerType, ownerId);
  }
}
