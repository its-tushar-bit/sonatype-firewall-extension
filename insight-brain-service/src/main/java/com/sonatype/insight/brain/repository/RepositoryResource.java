/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.integration.repository.RepositoryService;
import com.sonatype.insight.brain.integration.repository.RepositoryService.RepositoryDTO;

/**
 * @since 1.18.0
 */
@Named
@Path(RepositoryResource.RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryResource
{

  public static final String RESOURCE_PATH = "rest/repositories/{repositoryId}";

  private RepositoryService repositoryService;

  @Inject
  public RepositoryResource(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  @GET
  public RepositoryDTO getRepository(@PathParam("repositoryId") String repositoryId) {
    return repositoryService.getRepositoryById(repositoryId);
  }

  @POST
  @Path("evaluate")
  public void reevaluateRepository(@PathParam("repositoryId") String repositoryId) {
    repositoryService.reevaluateRepository(repositoryId);
  }
}
