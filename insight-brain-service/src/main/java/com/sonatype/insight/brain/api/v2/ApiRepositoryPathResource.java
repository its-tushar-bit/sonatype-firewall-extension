/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryPathResponseDTO;
import com.sonatype.insight.brain.api.v2.service.ApiRepositoryPathService;

import com.codahale.metrics.annotation.Timed;

/**
 * At the time of this writing, there is no public API documentation for this.  The main
 * purpose of this public api endpoint is to be used by NXRM npm audit.  This could be
 * publicly documented at a later date, if there was customer interest and the endpoint
 * isn't going to be abused.
 *
 * @since 1.125
 */
@Named
@Timed
@Path(PublicApiPaths.REPOSITORIES_RESOURCE_PATH)
public class ApiRepositoryPathResource
{
  static final String PATHNAMES_PATH =
      "{repositoryManagerInstanceId}/{repositoryPublicId}/components/quarantined/pathnames";

  private final ApiRepositoryPathService repositoryPathService;

  @Inject
  public ApiRepositoryPathResource(final ApiRepositoryPathService repositoryPathService) {
    this.repositoryPathService = repositoryPathService;
  }

  /**
   * @since 1.125
   */
  @POST
  @Path(PATHNAMES_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiRepositoryPathResponseDTO getQuarantinedByPath(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      List<String> pathnames)
  {
    return repositoryPathService.getQuarantinedByPathnames(repositoryManagerInstanceId, repositoryPublicId, pathnames);
  }
}
