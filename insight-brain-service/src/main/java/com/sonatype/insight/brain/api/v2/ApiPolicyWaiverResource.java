/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.sonatype.insight.brain.api.PublicApiPaths;
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

  @Inject
  public ApiPolicyWaiverResource(ApiPolicyWaiverService apiPolicyWaiverService) {
    this.apiPolicyWaiverService = apiPolicyWaiverService;
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
}
