/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.List;

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

import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentPathnames;
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
 * @since 1.17.0
 */
@Named
@Timed
@Path(RepositoryResource.RESOURCE_PATH)
public class RepositoryResource
    extends AbstractRepositoryResource
{
  public static final String RESOURCE_PATH = "rest/integration/repositories";

  static final String EVALUATE_COMPONENTS_ADHOC_PATH = REPOSITORY_PATH + "evaluate/adhoc";

  static final String IGNORE_PATTERNS_PATH = "evaluate/ignorePatterns";

  static final String REMOVE_EXTRA_COMPONENTS_PATH = REPOSITORY_PATH + "removeExtraComponents";

  private final RepositoryService repositoryService;

  private final FirewallIgnorePatternService firewallIgnorePatternService;

  @Inject
  public RepositoryResource(
      RepositoryService repositoryService,
      FirewallIgnorePatternService firewallIgnorePatternService)
  {
    this.repositoryService = repositoryService;
    this.firewallIgnorePatternService = firewallIgnorePatternService;
  }

  /**
   * Enable Audit for a repository. Both the repository manager and the repository may be known or unknown to the
   *  IQ server. If unknown, new entities are created in the IQ server database.
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
  public void evaluateComponents(@PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
                                 @PathParam("repositoryPublicId") String repositoryPublicId,
                                 RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
                                 @Context final HttpServletRequest request)
  {
    repositoryService.evaluateComponents(repositoryManagerInstanceId, repositoryPublicId,
        componentEvaluationDataRequestList, false, HdsClient.getClientUserAgent(request));
  }

  /**
   * Called from NXRM for npm audit. Maybe other usages?
   * 
   * @since 1.89
   */
  @POST
  @Path(EVALUATE_COMPONENTS_ADHOC_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_AD_HOC)
  @Timed
  public RepositoryComponentEvaluationDataList evaluateComponentsAdhoc(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      @Context HttpServletRequest request)
  {
    return repositoryService.evaluateComponentsAdhoc(repositoryManagerInstanceId, repositoryPublicId,
        componentEvaluationDataRequestList, HdsClient.getClientUserAgent(request));
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
    return repositoryService.evaluateComponents(repositoryManagerInstanceId, repositoryPublicId,
        componentEvaluationDataRequestList, true, HdsClient.getClientUserAgent(request));
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
   * - NXRM sends a list of hash+pathname pairs (all for the same component name) to IQ for policy evaluation.
   * - IQ picks up one hash+pathname pair and sends it to HDS.
   * - HDS finds the component identifier and name for the hash+pathname pair,
   * retrieves all variants for the component name and all the data associated with the variants (licenses, SVs, etc).
   * - IQ matches the data from HDS to the data from NXRM by hash+filename, runs policy evaluation for all variants,
   * determines which components would be quarantined and returns the results to NXRM.
   * 
   * @since 1.133
   */
  @POST
  @Path(EVALUATE_COMPONENT_METADATA_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public RepositoryComponentEvaluationDataList evaluateComponentMetadata(
      @PathParam("repositoryManagerInstanceId") final String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") final String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      @Context final HttpServletRequest request)
  {
    return repositoryService.evaluateComponentMetadata(repositoryManagerInstanceId, repositoryPublicId,
        componentEvaluationDataRequestList, HdsClient.getClientUserAgent(request));
  }

  @Path(QUARANTINE_PATH)
  @POST
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

  /**
   * @since 1.20
   */
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
   * @since 1.35
   */
  @GET
  @Path(IGNORE_PATTERNS_PATH)
  @Produces({ MediaType.APPLICATION_JSON })
  @Timed
  public FirewallIgnorePatterns getIgnorePatterns() {
    return firewallIgnorePatternService.getIgnorePatterns();
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
   * @since 1.106
   */
  @DELETE
  @Path(PROPRIETARY_NAMES_PATH)
  @Audited(AuditEvent.REMOVE_PROPRIETARY_COMPONENT_NAMES)
  @Timed
  public void removeProprietaryComponentNames(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId)
  {
    repositoryService.removeProprietaryComponentNames(repositoryManagerInstanceId, repositoryPublicId);
  }

  /**
   * @since 1.125
   */
  @GET
  @Path(QUARANTINED_COMPONENT_REPORT_URL_PATH)
  @Produces({MediaType.APPLICATION_JSON})
  @Timed
  public QuarantinedComponentReport getQuarantinedComponentReportUrl(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      @PathParam("pathname") String pathname,
      @Context final HttpServletRequest request)
  {
    return repositoryService
        .getQuarantinedComponentReportUrl(
            repositoryManagerInstanceId, repositoryPublicId, pathname,
            HdsClient.getClientUserAgent(request)
        );
  }

  /**
   * Removes all components from the given repository that have paths not in the given pathname list and with timestamp
   * before or equal to the given timestamp.
   * 
   * @param repositoryComponentPathnames the pathname list and timestamp used to filter the components to be deleted.
   * 
   * @since 1.137
   */
  @POST
  @Path(REMOVE_EXTRA_COMPONENTS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  public void removeExtraComponents(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      RepositoryComponentPathnames repositoryComponentPathnames)
  {
    repositoryService.removeExtraComponents(repositoryManagerInstanceId, repositoryPublicId,
        repositoryComponentPathnames);
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
