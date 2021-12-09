/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.repository.RepositoryPolicyThreatDTO;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.125.0
 */
@Named
@Timed
@Path(QuarantinedComponentResource.RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class QuarantinedComponentResource
{
  public static final String RESOURCE_PATH = "rest/repositories/quarantinedComponent/";

  public static final String QUARANTINED_COMPONENT_PATH = "{token}";

  public static final String QUARANTINED_COMPONENT_OVERVIEW_PATH = "{token}/overview";

  public static final String QUARANTINED_COMPONENT_POLICY_VIOLATIONS_PATH = "{token}/policyViolations";

  private final QuarantinedComponentService quarantinedComponentService;

  @Inject
  public QuarantinedComponentResource(final QuarantinedComponentService quarantinedComponentService) {
    this.quarantinedComponentService = quarantinedComponentService;
  }

  @GET
  @Path(QUARANTINED_COMPONENT_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public QuarantinedComponentDto getQuarantinedComponent(@PathParam("token") String token) {
    return quarantinedComponentService.getQuarantinedComponent(token);
  }

  @GET
  @Path(QUARANTINED_COMPONENT_OVERVIEW_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public QuarantinedComponentOverviewDto getQuarantinedComponentOverview(@PathParam("token") String token) {
    return quarantinedComponentService.getQuarantinedComponentOverview(token);
  }

  @GET
  @Path(QUARANTINED_COMPONENT_POLICY_VIOLATIONS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public RepositoryPolicyThreatDTO getQuarantinedComponentPolicyViolations(
      @PathParam("token") String token)
  {
    return quarantinedComponentService.getQuarantinedComponentPolicyViolations(token);
  }
}
