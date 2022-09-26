/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;

/**
 * Repository rest resource for integration with other tools
 *
 * @since 1.144.0
 */
@Named
@Timed
@Path(RepositorySummaryResource.RESOURCE_PATH)
public class RepositorySummaryResource
{
  public static final String RESOURCE_PATH = "rest/integration/repositories";

  private final RepositorySummaryService repositorySummaryService;

  @Inject
  public RepositorySummaryResource(RepositorySummaryService repositorySummaryService) {
    this.repositorySummaryService = repositorySummaryService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<RepositorySummary> getRepositories() {
    return repositorySummaryService.getRepositories();
  }
}
