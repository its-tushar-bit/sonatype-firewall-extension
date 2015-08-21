/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

/**
 * @since 1.17.0
 */
@Named
@Path(RepositoryResource.SERVICE_PATH)
public class RepositoryResource
{
  public static final String SERVICE_PATH = "rest/integration/repositories";

  private final RepositoryService repositoryService;

  @Inject
  public RepositoryResource(final RepositoryService repositoryService)
  {
    this.repositoryService = repositoryService;
  }

  /**
   * Enable a repository. Both the repository manager and the repository may be known or unknown to the IQ server. If
   * unknown, new entities are created in the IQ server database.
   */
  @POST
  @Path("{repositoryManagerInstanceId}/{repositoryPublicId}")
  @Consumes(MediaType.APPLICATION_JSON)
  public void enableRepository(@PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId)
  {
    repositoryService.enableRepository(repositoryManagerInstanceId, repositoryPublicId);
  }
}
