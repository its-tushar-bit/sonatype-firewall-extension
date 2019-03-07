/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.dto.repository.RepositoriesDTO;
import com.sonatype.insight.brain.dto.repository.RepositoryDTO;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.18.0
 */
@Named
@Timed
@Path(RepositoryResource.RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryResource
{
  public static final String RESOURCE_PATH = "rest/repositories";

  static final String REPOSITORY_PATH = "{repositoryId}";

  static final String EVALUATE_PATH = REPOSITORY_PATH + "/evaluate";

  static final String UNQUARANTINE_PATH = REPOSITORY_PATH + "/unquarantine/{pathname: .+}";

  static final String EVALUATE_COMPONENT_PATH = EVALUATE_PATH + "/{hash}";

  private RepositoryService repositoryService;

  @Inject
  public RepositoryResource(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  /**
   * @since 1.19.0
   */
  @POST
  @Path(UNQUARANTINE_PATH)
  @Audited(AuditEvent.RELEASE_QUARANTINE)
  public void unquarantineComponent(@PathParam("repositoryId") final String repositoryId,
                                    @PathParam("pathname") final String pathname,
                                    @Context final HttpServletRequest request)
  {
    repositoryService.unquarantineComponent(repositoryId, pathname, HdsClient.getClientUserAgent(request));
  }

  /**
   * @since 1.19.0
   */
  @GET
  public RepositoriesDTO getRepositories() {
    return repositoryService.getRepositories();
  }

  @GET
  @Path(REPOSITORY_PATH)
  public RepositoryDTO getRepository(@PathParam("repositoryId") String repositoryId) {
    return repositoryService.getRepositoryById(repositoryId);
  }

  /**
   * @since 1.19.0
   */
  @DELETE
  @Path(REPOSITORY_PATH)
  @Audited(AuditEvent.REMOVE_REPOSITORY)
  public void deleteRepository(@PathParam("repositoryId") String repositoryId) {
    repositoryService.deleteRepository(repositoryId);
  }

  @POST
  @Path(EVALUATE_PATH)
  @Audited(AuditEvent.INITIATE_EVALUATE_REPOSITORY)
  public void reevaluateRepository(@PathParam("repositoryId") String repositoryId) {
    repositoryService.reevaluateRepository(repositoryId);
  }

  @POST
  @Path(EVALUATE_COMPONENT_PATH)
  @Audited(AuditEvent.EVALUATE_REPOSITORY)
  public void reevaluateComponent(@PathParam("repositoryId") String repositoryId,
                                  @PathParam("hash") String componentHash,
                                  @Context final HttpServletRequest request)
  {
    repositoryService.reevaluateComponent(repositoryId, componentHash, HdsClient.getClientUserAgent(request));
  }
}
