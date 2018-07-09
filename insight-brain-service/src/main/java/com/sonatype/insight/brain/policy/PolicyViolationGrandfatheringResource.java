/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.codahale.metrics.annotation.Timed;

/**
 * @since POLICY_VIOLATION_GRANDFATHERING
 */
@Named
@Timed
@Path(PolicyViolationGrandfatheringResource.RESOURCE_PATH)
public class PolicyViolationGrandfatheringResource
{
  public static final String RESOURCE_PATH = "rest/policyViolationGrandfathering";

  public static final String REVOKE_PATH = "revoke/{applicationPublicId}";

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
}
