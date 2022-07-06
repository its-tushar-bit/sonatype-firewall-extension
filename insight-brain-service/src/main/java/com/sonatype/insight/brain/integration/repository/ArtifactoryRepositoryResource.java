/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.repository.QuarantinedComponentReport;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.hds.DefaultHdsClient;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.61.0
 */
@Named
@Timed
@Path(ArtifactoryRepositoryResource.RESOURCE_PATH)
public class ArtifactoryRepositoryResource
{
  static final String RESOURCE_PATH = "rest/integration/artifactory/repositories";

  private static final String REPOSITORY_PATH = "{repositoryManagerInstanceId}/{repositoryPublicId}/";

  static final String SUMMARY_PATH = REPOSITORY_PATH + "summary";

  static final String ENABLE_PATH = REPOSITORY_PATH + "enable/{enabled}";

  static final String QUARANTINE_PATH = REPOSITORY_PATH + "quarantine/{enabled}";

  static final String EVALUATE_COMPONENTS_PATH = REPOSITORY_PATH + "evaluate/audit";

  static final String COMPONENTS_PATH = REPOSITORY_PATH + "components/{pathname: .+}";

  static final String EVALUATE_COMPONENTS_WITH_QUARANTINE_PATH = REPOSITORY_PATH + "evaluate/quarantine";

  static final String UNQUARANTINED_COMPONENTS_PATH = REPOSITORY_PATH + "components/unquarantined";

  static final String PROPRIETARY_NAMES = REPOSITORY_PATH + "proprietary/names";

  static final String QUARANTINED_COMPONENT_REPORT_URL_PATH =
      REPOSITORY_PATH + "components/{pathname: .+}/quarantinedComponentReportUrl";

  private final ArtifactoryRepositoryServiceWrapper repositoryService;

  @Inject
  public ArtifactoryRepositoryResource(final ArtifactoryRepositoryServiceWrapper repositoryService) {
    this.repositoryService = repositoryService;
  }

  /**
   * Enable a repository. Both the repository manager and the repository may be known or unknown to the IQ server. If
   * unknown, new entities are created in the IQ server database.
   */
  @POST
  @Path(ENABLE_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  public void setEnabled(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      @PathParam("enabled") boolean enabled,
      @Context final HttpServletRequest request)
  {
    AuditData.get().setEvent(enabled ? AuditEvent.CONNECT_REPOSITORY : AuditEvent.DISCONNECT_REPOSITORY);
    repositoryService.setEnabled(repositoryManagerInstanceId, repositoryPublicId, enabled,
        DefaultHdsClient.getClientUserAgent(request));
  }

  @GET
  @Path(SUMMARY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary(
      @PathParam("repositoryManagerInstanceId") final String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") final String repositoryPublicId,
      @Context final HttpServletRequest request)
  {
    return repositoryService.getPolicyEvaluationSummary(repositoryManagerInstanceId, repositoryPublicId,
        DefaultHdsClient.getClientUserAgent(request));
  }

  @POST
  @Path(EVALUATE_COMPONENTS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_REPOSITORY)
  public void evaluateComponents(@PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
                                 @PathParam("repositoryPublicId") String repositoryPublicId,
                                 RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
                                 @Context final HttpServletRequest request)
  {
    repositoryService
        .evaluateComponents(repositoryManagerInstanceId, repositoryPublicId, componentEvaluationDataRequestList, false,
            DefaultHdsClient.getClientUserAgent(request));
  }

  @POST
  @Path(EVALUATE_COMPONENTS_WITH_QUARANTINE_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_REPOSITORY)
  public RepositoryComponentEvaluationDataList evaluateComponentsWithQuarantine(
      @PathParam("repositoryManagerInstanceId") final String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") final String repositoryPublicId,
      final RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      @Context final HttpServletRequest request)
  {
    return repositoryService
        .evaluateComponents(repositoryManagerInstanceId, repositoryPublicId, componentEvaluationDataRequestList, true,
            DefaultHdsClient.getClientUserAgent(request));
  }

  @Path(QUARANTINE_PATH)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_QUARANTINE)
  public void setQuarantine(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      @PathParam("enabled") boolean enabled,
      @Context final HttpServletRequest request)
  {
    repositoryService.setQuarantine(repositoryManagerInstanceId, repositoryPublicId, enabled,
        DefaultHdsClient.getClientUserAgent(request));
  }

  @DELETE
  @Path(COMPONENTS_PATH)
  public void removeComponent(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      @PathParam("pathname") String pathname,
      @Context final HttpServletRequest request)
  {
    repositoryService.removeComponent(repositoryManagerInstanceId, repositoryPublicId, pathname,
        DefaultHdsClient.getClientUserAgent(request));
  }

  @GET
  @Path(UNQUARANTINED_COMPONENTS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public UnquarantinedComponentList getUnquarantinedComponents(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      @QueryParam("sinceUtcTimestamp") long sinceUtcTimestamp,
      @Context final HttpServletRequest request)
  {
    return repositoryService.getUnquarantinedComponents(repositoryManagerInstanceId, repositoryPublicId,
        sinceUtcTimestamp, DefaultHdsClient.getClientUserAgent(request));
  }

  /**
   * @since 1.106
   */
  @POST
  @Path(PROPRIETARY_NAMES)
  @Consumes({MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.ADD_PROPRIETARY_COMPONENT_NAMES)
  public void addProprietaryComponentNames(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      ProprietaryComponentNames proprietaryComponentNames)
  {
    repositoryService.addProprietaryComponentNames(repositoryManagerInstanceId, repositoryPublicId,
        proprietaryComponentNames);
  }

  /**
   * @since 1.142
   */
  @GET
  @Path(QUARANTINED_COMPONENT_REPORT_URL_PATH)
  @Produces({MediaType.APPLICATION_JSON})
  @Timed
  public QuarantinedComponentReport getQuarantinedComponentReportUrl(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      @PathParam("pathname") String pathname,
      @Context HttpServletRequest request)
  {
    return repositoryService.getQuarantinedComponentReportUrl(repositoryManagerInstanceId, repositoryPublicId, pathname,
        DefaultHdsClient.getClientUserAgent(request));
  }
}
