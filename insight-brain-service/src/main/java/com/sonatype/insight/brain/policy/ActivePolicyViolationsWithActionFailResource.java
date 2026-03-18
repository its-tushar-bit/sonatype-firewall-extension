/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
@Timed
@Path(ActivePolicyViolationsWithActionFailResource.RESOURCE_PATH)
public class ActivePolicyViolationsWithActionFailResource
{
  public static final String RESOURCE_PATH =
      "rest/policy/violations/active/{applicationPublicId}/stages/{stageId}/action/fail";

  private final ActivePolicyViolationsWithActionFailService activePolicyViolationsWithActionFailService;

  private static final Logger log = LoggerFactory.getLogger(ActivePolicyViolationsWithActionFailResource.class);

  @Inject
  public ActivePolicyViolationsWithActionFailResource(
      ActivePolicyViolationsWithActionFailService activePolicyViolationsWithActionFailService)
  {
    this.activePolicyViolationsWithActionFailService = activePolicyViolationsWithActionFailService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<PolicyViolationWithoutConstraintFactsDTO> getActiveViolationsWithActionFail(
      @PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("stageId") String stageId)
  {
    log.debug(
        "Received request to get all active policy violations with action fail for {} id {}",
        applicationPublicId, stageId);

    List<PolicyViolationWithoutConstraintFactsDTO> policyViolations =
        activePolicyViolationsWithActionFailService.getActiveViolationsWithActionFail(
            applicationPublicId, stageId);

    return policyViolations;
  }
}
