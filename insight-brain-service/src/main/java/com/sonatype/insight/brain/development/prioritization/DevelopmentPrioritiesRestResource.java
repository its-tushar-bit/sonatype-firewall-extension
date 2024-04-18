/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.development.prioritization;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.experimental.development.prioritization.PrioritizedComponent;

import com.codahale.metrics.annotation.Timed;

// This will expose an endpoint which requires a CSRF token/user session for the web client to use
// The same logic is also exposed via the api for third parties to invoke with api style authentication
@Named
@Timed
@Path(DevelopmentPrioritiesRestResource.RESOURCE_PATH)
public class DevelopmentPrioritiesRestResource
{
  static final String RESOURCE_PATH = "rest/development/priorities/{applicationId}/{scanId}";

  private final DevelopmentPrioritiesService developmentPrioritiesService;

  @Inject
  DevelopmentPrioritiesRestResource(final DevelopmentPrioritiesService developmentPrioritiesService) {
    this.developmentPrioritiesService = developmentPrioritiesService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<PrioritizedComponent> getPriorities(
      @PathParam("applicationId") final String applicationId,
      @PathParam("scanId") final String scanId
  )
  {
    return developmentPrioritiesService.getPrioritizedFindings(applicationId, scanId);
  }
}
