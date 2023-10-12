/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallComponentDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineConfigDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationResultList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryManagerListDTO;
import com.sonatype.insight.brain.api.v2.dto.PaginationResponseBuilder;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.repository.FirewallFilterField;
import com.sonatype.insight.brain.dataaccess.repository.FirewallFilterField.FirewallFilterableField;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState;
import com.sonatype.insight.brain.dataaccess.repository.FirewallSortableField;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.106.0
 */
@Named
@Path(PublicApiPaths.FIREWALL_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class ApiFirewallResource
{
  static final String CONFIGURATION_PATH = "configuration";

  static final String RELEASE_QUARANTINE = "releaseQuarantine";

  static final String SUMMARY_PATH = "summary";

  static final String RELEASE_QUARANTINE_SUMMARY_PATH = RELEASE_QUARANTINE + "/" + SUMMARY_PATH;

  static final String RELEASE_QUARANTINE_CONFIGURATION_PATH = RELEASE_QUARANTINE + "/" + CONFIGURATION_PATH;

  static final String QUARANTINE_PATH = "quarantine";

  static final String QUARANTINE_SUMMARY_PATH = QUARANTINE_PATH + "/summary";

  static final String COMPONENTS_PATH = "/components";

  static final String UNQUARANTINE_PATH = COMPONENTS_PATH + "/autoReleasedFromQuarantine";

  static final String QUARANTINED_PATH = COMPONENTS_PATH + "/quarantined";

  static final String QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS =
      "quarantinedComponentView/configuration/anonymousAccess";

  static final String QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS_SET =
      QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS + "/{enabled: true|false}";

  static final String REPOSITORY_MANAGER_PATH = "repositoryManager";

  private static final String REPOSITORIES_PATH = "repositories";

  private static final String REPOSITORY_MANAGER_ID_PATH = "{repositoryManagerId}";

  private static final String REPOSITORY_ID_PATH = "{repositoryId}";

  // Visible for testing
  static final String REPOSITORIES_CONFIGURATION_PATH =
      REPOSITORIES_PATH + "/" + CONFIGURATION_PATH + "/" + REPOSITORY_MANAGER_ID_PATH;

  static final String EVALUATE_COMPONENTS_PATH =
      COMPONENTS_PATH + "/" + REPOSITORY_MANAGER_ID_PATH + "/" + REPOSITORY_ID_PATH + "/evaluate";

  private final ApiFirewallService apiFirewallService;

  @Inject
  public ApiFirewallResource(final ApiFirewallService apiFirewallService) {
    this.apiFirewallService = apiFirewallService;
  }

  @GET
  @Path(RELEASE_QUARANTINE_SUMMARY_PATH)
  public ApiFirewallReleaseQuarantineSummaryDTO getFirewallUnquarantineSummary() {
    return apiFirewallService.getReleaseQuarantineSummary();
  }

  @GET
  @Path(RELEASE_QUARANTINE_CONFIGURATION_PATH)
  public List<ApiFirewallReleaseQuarantineConfigDTO> getFirewallAutoUnquarantineConfig() {
    return apiFirewallService.getReleaseQuarantineConfig();
  }

  @PUT
  @Path(RELEASE_QUARANTINE_CONFIGURATION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_CONTINUOUS_MONITORING)
  public List<ApiFirewallReleaseQuarantineConfigDTO> setFirewallAutoUnquarantineConfig(
      final List<ApiFirewallReleaseQuarantineConfigDTO> apiFirewallReleaseQuarantineConfigDTOS)
  {
    return apiFirewallService.setReleaseQuarantineConfig(apiFirewallReleaseQuarantineConfigDTOS);
  }

  @GET
  @Path(QUARANTINE_SUMMARY_PATH)
  public ApiFirewallQuarantineSummaryDTO getQuarantineSummary() {
    return apiFirewallService.getQuarantineSummary();
  }

  @GET
  @Path(UNQUARANTINE_PATH)
  public Response getUnquarantineList(
      @Context UriInfo uriInfo,
      @DefaultValue("1") @QueryParam("page") int page,
      @DefaultValue("10") @QueryParam("pageSize") int pageSize,
      @QueryParam("policyId") String policyId,
      @QueryParam("componentName") String componentName,
      @QueryParam("sortBy") String sortBy,
      @DefaultValue("true") @QueryParam("asc") boolean asc
  )
  {
    return getComponents(uriInfo, page, pageSize, policyId != null ? Collections.singleton(policyId) : null,
        componentName, sortBy, FirewallSortableField.RELEASE_QUARANTINE_TIME, asc,
        FirewallComponentFilterState.UNQUARANTINE_AUTO);
  }

  @GET
  @Path(QUARANTINED_PATH)
  public Response getQuarantineList(
      @Context UriInfo uriInfo,
      @DefaultValue("1") @QueryParam("page") int page,
      @DefaultValue("10") @QueryParam("pageSize") int pageSize,
      @QueryParam("policyId") Set<String> policyIds,
      @QueryParam("componentName") String componentName,
      @QueryParam("sortBy") String sortBy,
      @DefaultValue("true") @QueryParam("asc") boolean asc
  )
  {
    return getComponents(uriInfo, page, pageSize, policyIds, componentName, sortBy,
        FirewallSortableField.QUARANTINE_TIME, asc, FirewallComponentFilterState.QUARANTINE);
  }

  /**
   * Enables/disables anonymous access to the Quarantined Component view
   * 
   * @since 1.136
   */
  @PUT
  @Path(QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS_SET)
  @Audited(AuditEvent.CONFIGURE_SECURITY_QUARANTINED_COMPONENT_VIEW_ANON_ACCESS)
  public void setQuarantinedComponentViewAnonymousAccess(
      @PathParam("enabled") boolean enabled)
  {
    apiFirewallService.setQuarantinedComponentViewAnonymousAccess(enabled);
  }

  /**
   * Returns whether Quarantine Component View can be accessed without authentication or not
   *
   * @since 1.136
   */
  @GET
  @Path(QUARANTINED_COMPONENT_VIEW_CONFIG_ANONYMOUS_ACCESS)
  public Response getQuarantinedComponentViewAnonymousAccess() {
    return Response.ok(apiFirewallService.getQuarantinedComponentViewAnonymousAccess()).build();
  }

  @GET
  @Path(REPOSITORY_MANAGER_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiRepositoryManagerListDTO getAllRepositoryManagers() {
    return apiFirewallService.getAllRepositoryManagers();
  }

  @GET
  @Path(REPOSITORIES_CONFIGURATION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ApiRepositoryListDTO getConfiguredRepositories(
      @PathParam("repositoryManagerId") String repositoryManagerId,
      @QueryParam("sinceUtcTimestamp") Long sinceUtcTimestamp)
  {
    return apiFirewallService.getConfiguredRepositories(repositoryManagerId, sinceUtcTimestamp);
  }

  @POST
  @Path(REPOSITORIES_CONFIGURATION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_REPOSITORY)
  public void configureRepositories(
      @PathParam("repositoryManagerId") String repositoryManagerId,
      ApiRepositoryListDTO dto)
  {
    apiFirewallService.configureRepositories(repositoryManagerId, dto);
  }

  @POST
  @Path(EVALUATE_COMPONENTS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.EVALUATE_REPOSITORY)
  public ApiRepositoryComponentEvaluationResultList evaluateComponents(
      @PathParam("repositoryManagerId") final String repositoryManagerId,
      @PathParam("repositoryId") final String repositoryId,
      final ApiRepositoryComponentEvaluationRequestList apiRepositoryComponentEvaluationRequestList)
  {
    return apiFirewallService.evaluateComponents(repositoryManagerId, repositoryId,
        apiRepositoryComponentEvaluationRequestList);
  }

  private Response getComponents(
      final UriInfo uriInfo,
      final int page,
      final int pageSize,
      final Set<String> policyIds,
      final String componentName,
      final String sortBy,
      final FirewallSortableField defaultSortableField,
      final boolean asc,
      final FirewallComponentFilterState firewallComponentFilterState)
  {
    List<FirewallFilterField> filterFields = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(policyIds)) {
      filterFields.add(new FirewallFilterField(FirewallFilterableField.POLICY_ID, policyIds));
    }

    if (StringUtils.isNotBlank(componentName)) {
      filterFields.add(new FirewallFilterField(FirewallFilterableField.COMPONENT_NAME, componentName));
    }

    final FirewallSortableField sortableField = sortBy == null ? defaultSortableField : initializeSortField(sortBy);

    final FirewallRepositoryComponentFilter firewallFilter =
        new FirewallRepositoryComponentFilter(page, pageSize, firewallComponentFilterState, sortableField,
            asc, filterFields);

    final ApiPageResult<ApiFirewallComponentDTO> result = apiFirewallService.getComponents(firewallFilter);

    return new PaginationResponseBuilder<>(uriInfo.getAbsolutePath().getPath(), page, pageSize, result)
        .queryParameters(uriInfo.getQueryParameters())
        .build();
  }

  private FirewallSortableField initializeSortField(final String sortBy) {
    final FirewallSortableField sortableField;
    try {
      sortableField = FirewallSortableField.getByLabel(sortBy);
    }
    catch (IllegalArgumentException exception) {
      throw new BadRequestException("sortBy field is invalid");
    }
    return sortableField;
  }
}
