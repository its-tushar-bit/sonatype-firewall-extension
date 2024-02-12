/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.resultsview;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.140.0
 */
@Named
@Timed
@Path(RepositoryResultsResource.RESOURCE_PATH)
public class RepositoryResultsResource
{
  public static final String RESOURCE_PATH = "api/experimental/repositories/{repositoryId}/results/details";

  private final RepositoryResultsService repositoryResultsService;

  @Inject
  public RepositoryResultsResource(final RepositoryResultsService repositoryResultsService) {
    this.repositoryResultsService = repositoryResultsService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_REPOSITORY_RESULTS)
  public RepositoryResultsDetailsResponseDto getDetails(
      @PathParam("repositoryId") final String repositoryId,
      final RepositoryResultsDetailsRequestDto detailsRequest)
  {
    return repositoryResultsService.getDetails(repositoryId, detailsRequest);
  }
}
