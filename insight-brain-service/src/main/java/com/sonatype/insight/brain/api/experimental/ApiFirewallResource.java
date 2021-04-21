/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallComponentDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallReleaseQuarantineConfigDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiFirewallReleaseQuarantineSummaryDTO;
import com.sonatype.insight.brain.api.experimental.dto.ApiPageResult;
import com.sonatype.insight.brain.api.experimental.dto.PaginationResponseBuilder;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.repository.FirewallFilterField;
import com.sonatype.insight.brain.dataaccess.repository.FirewallFilterField.FirewallFilterableField;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter;
import com.sonatype.insight.brain.dataaccess.repository.FirewallRepositoryComponentFilter.FirewallComponentFilterState;
import com.sonatype.insight.brain.dataaccess.repository.FirewallSortableField;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

import static com.sonatype.insight.brain.api.experimental.ApiFirewallResource.RESOURCE_PATH;

/**
 * @since 1.106.0
 */
@Named
@Path(RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class ApiFirewallResource
{
  static final String RESOURCE_PATH = PublicApiPaths.BASE_PATH + "/experimental/firewall";

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
      @QueryParam("sortBy") String sortBy,
      @DefaultValue("true") @QueryParam("asc") boolean asc
  )
  {
    return getComponents(uriInfo, page, pageSize, policyId, sortBy, FirewallSortableField.RELEASE_QUARANTINE_TIME, asc,
        FirewallComponentFilterState.UNQUARANTINE_AUTO);
  }

  @GET
  @Path(QUARANTINED_PATH)
  public Response getQuarantineList(
      @Context UriInfo uriInfo,
      @DefaultValue("1") @QueryParam("page") int page,
      @DefaultValue("10") @QueryParam("pageSize") int pageSize,
      @QueryParam("policyId") String policyId,
      @QueryParam("sortBy") String sortBy,
      @DefaultValue("true") @QueryParam("asc") boolean asc
  )
  {
    return getComponents(uriInfo, page, pageSize, policyId, sortBy, FirewallSortableField.QUARANTINE_TIME, asc,
        FirewallComponentFilterState.QUARANTINE);
  }

  private Response getComponents(
      final UriInfo uriInfo,
      final int page,
      final int pageSize,
      final String policyId,
      final String sortBy,
      final FirewallSortableField defaultSortableField,
      final boolean asc,
      final FirewallComponentFilterState firewallComponentFilterState)
  {
    List<FirewallFilterField> filterFields = new ArrayList<>();
    if (!StringUtils.isEmpty(policyId)) {
      filterFields.add(new FirewallFilterField(FirewallFilterableField.POLICY_ID, policyId));
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
