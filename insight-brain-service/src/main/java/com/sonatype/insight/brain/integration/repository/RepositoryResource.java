/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;

import com.yammer.metrics.annotation.Timed;

/**
 * @since 1.17.0
 */
@Named
@Path(RepositoryResource.RESOURCE_PATH)
public class RepositoryResource
{

  public static final String RESOURCE_PATH = "rest/integration/repositories/{repositoryManagerInstanceId}/{repositoryPublicId}";

  public static final String SUMMARY_PATH = "summary";

  static final String QUARANTINE_PATH = "quarantine/{enabled}";

  public static final String EVALUATE_COMPONENTS_PATH = "evaluate/audit";

  static final String COMPONENTS_PATH = "components/{pathname: .+}";

  public static final String EVALUATE_COMPONENT_WITH_QUARANTINE_PATH = "evaluate/quarantine";

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
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  public void enableRepository(@PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId)
  {
    repositoryService.enableRepository(repositoryManagerInstanceId, repositoryPublicId);
  }

  @GET
  @Path(SUMMARY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary(
      @PathParam("repositoryManagerInstanceId") final String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") final String repositoryPublicId)
  {
    return repositoryService.getPolicyEvaluationSummary(repositoryManagerInstanceId, repositoryPublicId);
  }

  @POST
  @Path(EVALUATE_COMPONENTS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  public void evaluateComponents(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    repositoryService.evaluateComponents(repositoryManagerInstanceId, repositoryPublicId,
        componentEvaluationDataRequestList, false);
  }

  @POST
  @Path(EVALUATE_COMPONENT_WITH_QUARANTINE_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public RepositoryComponentEvaluationDataList evaluateComponentWithQuarantine(
      @PathParam("repositoryManagerInstanceId") final String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") final String repositoryPublicId,
      final RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    return repositoryService.evaluateComponents(repositoryManagerInstanceId, repositoryPublicId,
        componentEvaluationDataRequestList, true);
  }

  @Path(QUARANTINE_PATH)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  public void setQuarantine(@PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId, @PathParam("enabled") boolean enabled)
  {
    repositoryService.setQuarantine(repositoryManagerInstanceId, repositoryPublicId, enabled);
  }

  @DELETE
  @Path(COMPONENTS_PATH)
  @Timed
  public void removeComponent(@PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId, @PathParam("pathname") String pathname)
  {
    repositoryService.removeComponent(repositoryManagerInstanceId, repositoryPublicId, pathname);
  }
}
