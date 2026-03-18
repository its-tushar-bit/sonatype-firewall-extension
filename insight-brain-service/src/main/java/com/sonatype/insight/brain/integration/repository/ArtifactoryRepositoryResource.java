/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.clm.dto.model.repository.QuarantinedComponentReport;
import com.sonatype.clm.dto.model.repository.RepositoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.hds.HdsClient;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.61.0
 */
@Named
@Timed
@Path(ArtifactoryRepositoryResource.RESOURCE_PATH)
public class ArtifactoryRepositoryResource
    extends AbstractRepositoryResource
{
  static final String RESOURCE_PATH = "rest/integration/artifactory/repositories";

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
  @Path(AUDIT_ENABLE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public ApiRepositoryDTO setAuditEnabled(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      @PathParam("enabled") boolean auditEnabled,
      @Context final HttpServletRequest request)
  {
    AuditData.get().setEvent(auditEnabled ? AuditEvent.CONNECT_REPOSITORY : AuditEvent.DISCONNECT_REPOSITORY);
    return repositoryService.setAuditEnabled(repositoryManagerInstanceId, repositoryPublicId, auditEnabled,
        HdsClient.getClientUserAgent(request));
  }

  @GET
  @Path(SUMMARY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary(
      @PathParam("repositoryManagerInstanceId") final String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") final String repositoryPublicId,
      @Context final HttpServletRequest request)
  {
    return repositoryService.getPolicyEvaluationSummary(repositoryManagerInstanceId, repositoryPublicId,
        HdsClient.getClientUserAgent(request));
  }

  @GET
  @Path(REPOSITORY_RESULTS_URL)
  @Produces(MediaType.TEXT_PLAIN)
  @Timed
  public String getRepositoryResultsUrl(
      @PathParam("repositoryManagerInstanceId") final String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") final String repositoryPublicId,
      @Context final HttpServletRequest request)
  {
    return repositoryService.getRepositoryResultsUrl(repositoryManagerInstanceId, repositoryPublicId,
        HdsClient.getClientUserAgent(request));
  }

  @POST
  @Path(EVALUATE_COMPONENTS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_REPOSITORY)
  @Timed
  public void evaluateComponents(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      @Context final HttpServletRequest request)
  {
    repositoryService
        .evaluateComponents(repositoryManagerInstanceId, repositoryPublicId, componentEvaluationDataRequestList, false,
            HdsClient.getClientUserAgent(request));
  }

  @POST
  @Path(EVALUATE_COMPONENTS_WITH_QUARANTINE_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_REPOSITORY)
  @Timed
  public RepositoryComponentEvaluationDataList evaluateComponentsWithQuarantine(
      @PathParam("repositoryManagerInstanceId") final String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") final String repositoryPublicId,
      final RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      @Context final HttpServletRequest request)
  {
    return repositoryService
        .evaluateComponents(repositoryManagerInstanceId, repositoryPublicId, componentEvaluationDataRequestList, true,
            HdsClient.getClientUserAgent(request));
  }

  @Path(QUARANTINE_PATH)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_QUARANTINE)
  @Timed
  public void setQuarantine(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      @PathParam("enabled") boolean enabled,
      @Context final HttpServletRequest request)
  {
    repositoryService.setQuarantine(repositoryManagerInstanceId, repositoryPublicId, enabled,
        HdsClient.getClientUserAgent(request));
  }

  @DELETE
  @Path(COMPONENTS_PATH)
  @Timed
  public void removeComponent(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      @PathParam("pathname") String pathname,
      @Context final HttpServletRequest request)
  {
    repositoryService.removeComponent(repositoryManagerInstanceId, repositoryPublicId, pathname,
        HdsClient.getClientUserAgent(request));
  }

  @GET
  @Path(UNQUARANTINED_COMPONENTS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public UnquarantinedComponentList getUnquarantinedComponents(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      @QueryParam("sinceUtcTimestamp") long sinceUtcTimestamp,
      @Context final HttpServletRequest request)
  {
    return repositoryService.getUnquarantinedComponents(repositoryManagerInstanceId, repositoryPublicId,
        sinceUtcTimestamp, HdsClient.getClientUserAgent(request));
  }

  /**
   * @since 1.106
   */
  @POST
  @Path(PROPRIETARY_NAMES_PATH)
  @Consumes({MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.ADD_PROPRIETARY_COMPONENT_NAMES)
  @Timed
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
        HdsClient.getClientUserAgent(request));
  }

  /**
   * Evaluates policies on variants of the same component.
   * The specified componentEvaluationDataRequestList must contain only variants of the same component
   * Only the npm and pypi formats are supported.
   *
   * It is very important for performance to minimize the number of round trips between:
   * - IQ and HDS
   * - IQ and the IQ ODS db
   * - HDS and HDS dm db
   *
   * How it works:
   * - Artifactory sends a list of hash+pathname pairs (all for the same component name) to IQ for policy evaluation.
   * - IQ picks up one hash+pathname pair and sends it to HDS.
   * - HDS finds the component identifier and name for the hash+pathname pair,
   * retrieves all variants for the component name and all the data associated with the variants (licenses, SVs, etc).
   * - IQ matches the data from HDS to the data from Artifactory by hash+filename, runs policy evaluation for all
   * variants, determines which components would be quarantined and returns the results to Artifactory.
   *
   * @since 1.145
   */
  @POST
  @Path(EVALUATE_COMPONENT_METADATA_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public RepositoryComponentEvaluationDataList evaluateComponentMetadata(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      @Context HttpServletRequest request)
  {
    return repositoryService.evaluateComponentMetadata(repositoryManagerInstanceId, repositoryPublicId,
        componentEvaluationDataRequestList, HdsClient.getClientUserAgent(request));
  }

  /**
   * @since 1.160
   */
  @POST
  @Path(CONFIGURE_REPOSITORIES_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_REPOSITORY)
  @Timed
  public void configureRepositories(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      ConfigureRepositoriesRequest configureRepositoriesRequest,
      @Context HttpServletRequest request)
  {
    repositoryService.configureRepositories(repositoryManagerInstanceId, configureRepositoriesRequest,
        HdsClient.getClientUserAgent(request));
  }

  /**
   * @since 1.161
   */
  @DELETE
  @Path(REPOSITORY_PATH)
  @Audited(AuditEvent.REMOVE_REPOSITORY)
  @Timed
  public void removeRepository(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId)
  {
    repositoryService.removeRepository(repositoryManagerInstanceId, repositoryPublicId);
  }

  /**
   * @since 1.161
   */
  @GET
  @Path(GET_CONFIGURED_REPOSITORIES_PATH)
  @Produces({MediaType.APPLICATION_JSON})
  @Timed
  public List<RepositoryDTO> getConfiguredRepositories(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @QueryParam("sinceUtcTimestamp") Long sinceUtcTimestamp,
      @Context final HttpServletRequest request)
  {
    return repositoryService.getConfiguredRepositories(repositoryManagerInstanceId, sinceUtcTimestamp,
        HdsClient.getClientUserAgent(request));
  }
}
