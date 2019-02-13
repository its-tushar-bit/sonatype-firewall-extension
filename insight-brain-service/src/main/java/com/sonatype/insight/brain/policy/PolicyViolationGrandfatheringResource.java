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

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.policy.PolicyViolationGrandfatheringService.PolicyViolationGrandfatheringDTO;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.50
 */
@Named
@Timed
@Path(PolicyViolationGrandfatheringResource.RESOURCE_PATH)
public class PolicyViolationGrandfatheringResource
{
  static final String RESOURCE_PATH = "rest/policyViolationGrandfathering";

  static final String REVOKE_PATH = "revoke/{applicationPublicId}";

  static final String GRANDFATHER_PATH = "grandfather/{applicationPublicId}";

  static final String GET_PATH = "{ownerType: application|organization}/{ownerId}";

  private PolicyViolationGrandfatheringService policyViolationGrandfatheringService;

  @Inject
  @SuppressWarnings("checkstyle:LineLength")
  public PolicyViolationGrandfatheringResource(PolicyViolationGrandfatheringService policyViolationGrandfatheringService) {
    this.policyViolationGrandfatheringService = policyViolationGrandfatheringService;
  }

  @PUT
  @Path(REVOKE_PATH)
  @Audited(AuditEvent.REVOKE_GRANDFATHERING)
  public void revokeGrandfathering(@PathParam("applicationPublicId") String applicationPublicId) {
    policyViolationGrandfatheringService.revokeGrandfathering(applicationPublicId);
  }

  @PUT
  @Path(GRANDFATHER_PATH)
  @Audited(AuditEvent.APPLY_GRANDFATHERING)
  public void grandfather(@PathParam("applicationPublicId") String applicationPublicId) {
    policyViolationGrandfatheringService.grandfather(applicationPublicId);
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
  @Audited(AuditEvent.CONFIGURE_GRANDFATHERING)
  @SuppressWarnings("checkstyle:LineLength")
  public PolicyViolationGrandfatheringDTO setGrandfathering(@PathParam("ownerType") OwnerType ownerType,
                                                            @PathParam("ownerId") String ownerId,
                                                            PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO)
  {
    return policyViolationGrandfatheringService.setGrandfathering(ownerType, ownerId, policyViolationGrandfatheringDTO);
  }
}
