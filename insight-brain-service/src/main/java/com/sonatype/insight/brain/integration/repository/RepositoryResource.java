/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import com.codahale.metrics.annotation.Timed;
import com.sonatype.clm.dto.model.component.FirewallIgnorePatterns;
import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentPathnames;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationSummary;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.clm.dto.model.repository.QuarantinedComponentReport;
import com.sonatype.clm.dto.model.repository.RepositoryDTO;
import com.sonatype.clm.dto.model.repository.container.image.FirewallContainerImageEvaluationResponse;
import com.sonatype.insight.brain.api.v2.HasFeature;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
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
import java.util.List;

/**
 * @since 1.17.0
 */
@Named("integrationRepositoryResource")
@Timed
@Path(RepositoryResource.RESOURCE_PATH)
public class RepositoryResource
    extends AbstractRepositoryResource
{
  public static final String RESOURCE_PATH = "rest/integration/repositories";

  static final String EVALUATE_COMPONENTS_ADHOC_PATH = REPOSITORY_PATH + "evaluate/adhoc";

  static final String IGNORE_PATTERNS_PATH = "evaluate/ignorePatterns";

  static final String REMOVE_EXTRA_COMPONENTS_PATH = REPOSITORY_PATH + "removeExtraComponents";

  static final String IS_QUARANTINED_CONTAINER_IMAGE_PATH =
      REPOSITORY_PATH + "containerImage/{containerImagePublicId}/isQuarantined";

  static final String EVALUATE_CONTAINER_IMAGE_PATH = REPOSITORY_PATH + "evaluate/containerImage";

  static final String EVALUATION_STATUS_CONTAINER_IMAGE_PATH =
      "containerImage/{containerImagePublicId}/evaluation/status/{statusId}";

  static final String CONTAINER_IMAGE_REPORT_PATH =
      REPOSITORY_PATH + "containerImage/{containerImageIdOrPublicId}/report";

  static final String GET_AVAILABLE_FORMATS_PATH = "repositoryManager/{repositoryManagerInstanceId}/availableFormats";

  static final String UI_CONFIGURED_REPOSITORIES_PATH =
      "{repositoryManagerInstanceId}/ui/configuredRepositories";

  private final RepositoryService repositoryService;

  private final FirewallIgnorePatternService firewallIgnorePatternService;

  private final RepositoryContainerImageService repositoryContainerImageService;

  @Inject
  public RepositoryResource(
      RepositoryService repositoryService,
      FirewallIgnorePatternService firewallIgnorePatternService,
      RepositoryContainerImageService repositoryContainerImageService)
  {
    this.repositoryService = repositoryService;
    this.firewallIgnorePatternService = firewallIgnorePatternService;
    this.repositoryContainerImageService = repositoryContainerImageService;
  }

  /**
   * Enable Audit for a repository. Both the repository manager and the repository may be known or unknown to the
   * IQ server. If unknown, new entities are created in the IQ server database.
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
  @Produces({MediaType.APPLICATION_JSON})
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
            HdsClient.getClientUserAgent(request));
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

  /**
   * @since 1.169
   */
  @GET
  @Path(UI_CONFIGURED_REPOSITORIES_PATH)
  @Produces({MediaType.APPLICATION_JSON})
  @Timed
  @HasFeature(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)
  public HostedRepositoryListDTO getUiConfiguredRepositories(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @QueryParam("searchText") String searchText,
      @QueryParam("format") String format,
      @QueryParam("sortBy") String sortBy,
      @QueryParam("sortDir") String sortDir,
      @QueryParam("page") Integer page,
      @QueryParam("pageSize") Integer pageSize)
  {
    return repositoryService.getConfiguredRepositories(repositoryManagerInstanceId, searchText, format, sortBy,
        sortDir, page, pageSize);
  }

  /**
   * @since 1.169
   */
  @GET
  @Path(GET_AVAILABLE_FORMATS_PATH)
  @Produces({MediaType.APPLICATION_JSON})
  @Timed
  @HasFeature(SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION)
  public List<String> getAvailableFormats(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId)
  {
    return repositoryService.getAvailableFormats(repositoryManagerInstanceId);
  }

  @GET
  @Path(IS_QUARANTINED_CONTAINER_IMAGE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  @ProductLicenseEnforcementPoint(LicensedFeature.CONTAINER_IMAGES_EVALUATION)
  @HasFeature(SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED)
  public boolean isContainerImageQuarantined(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      @PathParam("containerImagePublicId") String containerImagePublicId)
  {
    return repositoryContainerImageService.isContainerImageQuarantined(repositoryManagerInstanceId, repositoryPublicId,
        containerImagePublicId);
  }

  @POST
  @Path(EVALUATE_CONTAINER_IMAGE_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_REPOSITORY)
  @Timed
  @ProductLicenseEnforcementPoint(LicensedFeature.CONTAINER_IMAGES_EVALUATION)
  @HasFeature(SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED)
  public FirewallContainerImageEvaluationResponse evaluateContainerImage(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      String cycloneDxBomJson,
      @Context HttpServletRequest request)
  {
    return repositoryContainerImageService.evaluateContainerImage(repositoryManagerInstanceId, repositoryPublicId,
        cycloneDxBomJson, HdsClient.getClientUserAgent(request));
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(EVALUATION_STATUS_CONTAINER_IMAGE_PATH)
  @Timed
  @ProductLicenseEnforcementPoint(LicensedFeature.CONTAINER_IMAGES_EVALUATION)
  @HasFeature(SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED)
  public PolicyEvaluationPollingResult pollContainerImageEvaluationResult(
      @PathParam("containerImagePublicId") String containerImagePublicId,
      @PathParam("statusId") String statusId)
  {
    return repositoryContainerImageService.pollContainerImageEvaluationResult(containerImagePublicId, statusId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(CONTAINER_IMAGE_REPORT_PATH)
  @Timed
  @ProductLicenseEnforcementPoint(LicensedFeature.CONTAINER_IMAGES_EVALUATION)
  @HasFeature(SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED)
  public PolicyEvaluationSummary getContainerImageReportUrl(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      @PathParam("containerImageIdOrPublicId") String containerImageIdOrPublicId)
  {
    return repositoryContainerImageService.getContainerImageReportUrl(repositoryManagerInstanceId, repositoryPublicId,
        containerImageIdOrPublicId);
  }

  @GET
  @Path(IS_COMPONENT_WAIVED_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  public boolean isComponentWaived(
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @PathParam("repositoryPublicId") String repositoryPublicId,
      @QueryParam("pathname") @NotBlank String pathname)
  {
    return repositoryService.isMalwareWaived(repositoryManagerInstanceId, repositoryPublicId, pathname);
  }
}
