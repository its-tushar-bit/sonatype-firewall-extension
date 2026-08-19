/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.development.prioritization;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;

// This will expose an endpoint which requires a CSRF token/user session for the web client to use
// The same logic is also exposed via the api for third parties to invoke with api style authentication
@Named
@Timed
@Path(DevelopmentPrioritiesRestResource.RESOURCE_PATH)
public class DevelopmentPrioritiesRestResource
{
  static final String DEFAULT_PAGE = "1";

  static final String DEFAULT_PAGE_SIZE = "10";

  static final String RESOURCE_PATH = "rest/developer/priorities/{applicationId}/{scanId}";

  private final DevelopmentPrioritiesService developmentPrioritiesService;

  private final ApplicationDAO applicationDAO;

  @Inject
  DevelopmentPrioritiesRestResource(
      final DevelopmentPrioritiesService developmentPrioritiesService,
      final ApplicationDAO applicationDAO)
  {
    this.developmentPrioritiesService = developmentPrioritiesService;
    this.applicationDAO = applicationDAO;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public DevelopmentPrioritizationResults getPriorities(
      @PathParam("applicationId") final String applicationId,
      @PathParam("scanId") final String scanId,
      @DefaultValue(DEFAULT_PAGE) @QueryParam("page") final int page,
      @DefaultValue(DEFAULT_PAGE_SIZE) @QueryParam("pageSize") final int pageSize,
      @QueryParam("componentNameFilter") final String componentNameFilter,
      @QueryParam("filterOnPolicyActions") @DefaultValue("true") final boolean filterOnPolicyActions)
  {
    return developmentPrioritiesService
        .getPrioritizedFindings(applicationDAO.getByPublicIdNotNull(applicationId), scanId, page, pageSize,
            componentNameFilter, false, filterOnPolicyActions);
  }
}
