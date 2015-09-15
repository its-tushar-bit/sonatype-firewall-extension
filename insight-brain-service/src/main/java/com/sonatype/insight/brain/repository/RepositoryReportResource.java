/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.integration.repository.RepositoryService;

/**
 * @since 1.17.0
 */
@Named
@Path(RepositoryReportResource.RESOURCE_PATH)
public class RepositoryReportResource
{
  public static final String SUMMARY = "summary";

  public static final String RESOURCE_PATH = "rest/repositories/{repositoryManagerInstanceId}/{repositoryPublicId}/report";

  private final RepositoryService repositoryService;

  @Inject
  public RepositoryReportResource(final RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  @GET
  @Path(SUMMARY)
  @Produces(MediaType.APPLICATION_JSON)
  public RepositoryReportSummary getSummary(@PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId)
  {
    return repositoryService.getReportSummary(repositoryManagerInstanceId, repositoryPublicId);
  }

  public static class RepositoryReportSummary
  {
    public int knownComponentCount;

    public int totalComponentCount;

    public int criticalComponentCount;

    public int severeComponentCount;

    public int moderateComponentCount;

    public int affectedComponentCount;
  }
}
