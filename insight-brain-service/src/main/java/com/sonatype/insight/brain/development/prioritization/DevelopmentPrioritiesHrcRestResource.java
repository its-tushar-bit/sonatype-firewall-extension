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

import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.dataaccess.repository.HostedRepositoryComponentDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import com.codahale.metrics.annotation.Timed;

/**
 * HRC-scoped sibling of {@link DevelopmentPrioritiesRestResource}. Resolves the
 * {@code HostedRepositoryComponent} owner and delegates through the shared owner-scoped
 * {@link DevelopmentPrioritiesService#getPrioritizedFindings} used by the app path.
 */
@Named
@Timed
@Path(DevelopmentPrioritiesHrcRestResource.RESOURCE_PATH)
@HasFeature(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)
public class DevelopmentPrioritiesHrcRestResource
{
  static final String DEFAULT_PAGE = "1";

  static final String DEFAULT_PAGE_SIZE = "10";

  static final String RESOURCE_PATH = "rest/developer/priorities/hostedRepositoryComponent/{hrcId}/{scanId}";

  private final DevelopmentPrioritiesService developmentPrioritiesService;

  private final HostedRepositoryComponentDAO hostedRepositoryComponentDAO;

  @Inject
  DevelopmentPrioritiesHrcRestResource(
      final DevelopmentPrioritiesService developmentPrioritiesService,
      final HostedRepositoryComponentDAO hostedRepositoryComponentDAO)
  {
    this.developmentPrioritiesService = developmentPrioritiesService;
    this.hostedRepositoryComponentDAO = hostedRepositoryComponentDAO;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public DevelopmentPrioritizationResults getPriorities(
      @PathParam("hrcId") final String hrcId,
      @PathParam("scanId") final String scanId,
      @DefaultValue(DEFAULT_PAGE) @QueryParam("page") final int page,
      @DefaultValue(DEFAULT_PAGE_SIZE) @QueryParam("pageSize") final int pageSize,
      @QueryParam("componentNameFilter") final String componentNameFilter,
      @QueryParam("filterOnPolicyActions") @DefaultValue("true") final boolean filterOnPolicyActions)
  {
    return developmentPrioritiesService
        .getPrioritizedFindings(hostedRepositoryComponentDAO.getByIdNotNull(hrcId), scanId, page, pageSize,
            componentNameFilter, false, filterOnPolicyActions);
  }
}
