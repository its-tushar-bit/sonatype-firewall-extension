/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.policy.PolicyViolationGrandfatheringService.PolicyViolationGrandfatheringDTO;

import com.codahale.metrics.annotation.Timed;

/**
 * @since POLICY_VIOLATION_GRANDFATHERING
 */
@Named
@Timed
@Path(PolicyViolationGrandfatheringResource.RESOURCE_PATH)
public class PolicyViolationGrandfatheringResource
{
  static final String RESOURCE_PATH = "rest/policyViolationGrandfathering";

  static final String REVOKE_PATH = "revoke/{applicationPublicId}";

  static final String GET_PATH = "{ownerType: application|organization}/{ownerId}";

  private PolicyViolationGrandfatheringService policyViolationGrandfatheringService;

  @Inject
  public PolicyViolationGrandfatheringResource(PolicyViolationGrandfatheringService policyViolationGrandfatheringService) {
    this.policyViolationGrandfatheringService = policyViolationGrandfatheringService;
  }

  @PUT
  @Path(REVOKE_PATH)
  public void revokeGrandfathering(@PathParam("applicationPublicId") String applicationPublicId) {
    policyViolationGrandfatheringService.revokeGrandfathering(applicationPublicId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(GET_PATH)
  public PolicyViolationGrandfatheringDTO getGrandfathering(@PathParam("ownerType") OwnerType ownerType,
                                                            @PathParam("ownerId") String ownerId)
  {
    return policyViolationGrandfatheringService.getGrandfathering(ownerType, ownerId);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path(GET_PATH)
  public PolicyViolationGrandfatheringDTO setGrandfathering(@PathParam("ownerType") OwnerType ownerType,
                                                            @PathParam("ownerId") String ownerId,
                                                            PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO)
  {
    return policyViolationGrandfatheringService.setGrandfathering(ownerType, ownerId, policyViolationGrandfatheringDTO);
  }
}
