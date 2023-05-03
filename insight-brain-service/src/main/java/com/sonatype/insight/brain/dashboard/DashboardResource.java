/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.utils.Csv;
import com.sonatype.insight.error.exception.BadRequestException;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Named
@Timed
@Path(DashboardResource.RESOURCE_PATH)
public class DashboardResource
{
  public static final String RESOURCE_PATH = "rest/dashboard";

  public static final String GET_NEWEST_RISKS_PATH = "policy/newestRisks";

  public static final String GET_NEWEST_RISKS_EXPORT_PATH = "export/newestRisks";

  public static final String GET_COMPONENT_RISKS_PATH = "policy/componentRisks";

  public static final String GET_COMPONENT_RISKS_EXPORT_PATH = "export/componentRisks";

  public static final String GET_APPLICATION_RISKS_PATH = "policy/applicationRisks";

  public static final String GET_APPLICATION_RISKS_EXPORT_PATH = "export/applicationRisks";

  public static final String GET_POLICY_WAIVERS_PATH = "policy/policyWaivers";

  public static final String GET_POLICY_WAIVERS_EXPORT_PATH = "export/policyWaivers";

  public static final String FILTERS_PATH = "filters/active";

  public static final String NAMED_FILTERS_PATH = "filters/named";

  public static final String DELETE_NAMED_FILTER_PATH = NAMED_FILTERS_PATH + "/delete";

  private final ApplicationRiskService applicationRiskService;

  private final ComponentRiskService componentRiskService;

  private final DashboardFilterService dashboardFilterService;

  private final NewestRiskService newestRiskService;

  private final DashboardPolicyWaiverService dashboardPolicyWaiverService;

  @Inject
  public DashboardResource(
      ApplicationRiskService applicationRiskService,
      DashboardFilterService dashboardFilterService,
      ComponentRiskService componentRiskService,
      NewestRiskService newestRiskService,
      DashboardPolicyWaiverService dashboardPolicyWaiverService)
  {
    this.applicationRiskService = applicationRiskService;
    this.componentRiskService = componentRiskService;
    this.dashboardFilterService = dashboardFilterService;
    this.newestRiskService = newestRiskService;
    this.dashboardPolicyWaiverService = dashboardPolicyWaiverService;
  }

  @POST
  @Path(GET_NEWEST_RISKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getNewestRisksExceptionMeter")
  @Audited(AuditEvent.VIEW_DASHBOARD_VIOLATION_LIST)
  public DashboardResultsDTO<NewestRiskDTO> getNewestRisks(RisksFilterDTO risksFilterDTO) {
    return newestRiskService.getNewestRisks(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds,
        risksFilterDTO.stageIds, risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories,
        risksFilterDTO.policyThreatLevelRange, risksFilterDTO.policyViolationStates, risksFilterDTO.orderBy,
        risksFilterDTO.maxDaysOld, risksFilterDTO.page, risksFilterDTO.pageSize);
  }

  @POST
  @Path(GET_APPLICATION_RISKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getApplicationRisksExceptionMeter")
  @Audited(AuditEvent.VIEW_DASHBOARD_APPLICATION_LIST)
  public DashboardResultsDTO<ApplicationRiskScoreDTO> getApplicationRisks(RisksFilterDTO risksFilterDTO) {
    return applicationRiskService.getApplicationRisks(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds,
        risksFilterDTO.stageIds, risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories,
        risksFilterDTO.policyThreatLevelRange, risksFilterDTO.policyViolationStates, risksFilterDTO.orderBy,
        risksFilterDTO.maxResults);
  }

  @POST
  @Path(GET_COMPONENT_RISKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getComponentRisksExceptionMeter")
  @Audited(AuditEvent.VIEW_DASHBOARD_COMPONENT_LIST)
  public DashboardResultsDTO<ComponentRiskDTO> getComponentRisks(RisksFilterDTO risksFilterDTO) {
    return componentRiskService.getComponentRisks(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds,
        risksFilterDTO.stageIds, risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories,
        risksFilterDTO.policyThreatLevelRange, risksFilterDTO.policyViolationStates, risksFilterDTO.orderBy,
        risksFilterDTO.maxResults);
  }

  /**
   * @since 1.11.0
   */
  @GET
  @Path(FILTERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getActiveDashboardFilterForCurrentUserExceptionMeter")
  public NamedDashboardFilterDTO getActiveDashboardFilterForCurrentUser() throws IOException {
    return dashboardFilterService.getActiveDashboardFilterForCurrentUser();
  }

  /**
   * @since 1.24.0
   */
  @GET
  @Path(NAMED_FILTERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getNamedDashboardFiltersForCurrentUserExceptionMeter")
  public List<NamedDashboardFilterDTO> getNamedDashboardFiltersForCurrentUser() throws IOException {
    return dashboardFilterService.getNamedDashboardFiltersForCurrentUser();
  }

  /**
   * @since 1.11.0
   */
  @PUT
  @Path(FILTERS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "updateDashboardFilterForCurrentUserExceptionMeter")
  @Audited(AuditEvent.SAVE_DASHBOARD_FILTER)
  public DashboardFilterDTO updateDashboardFilterForCurrentUser(NamedDashboardFilterDTO namedDashboardFilterDTO) {
    namedDashboardFilterDTO.name = "";
    return dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(namedDashboardFilterDTO).filter;
  }

  /**
   * @since 1.24.0
   */
  @PUT
  @Path(NAMED_FILTERS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "createOrUpdateDashboardFilterForCurrentUserExceptionMeter")
  @Audited(AuditEvent.SAVE_DASHBOARD_FILTER)
  public NamedDashboardFilterDTO createOrUpdateDashboardFilterForCurrentUser(
      NamedDashboardFilterDTO dashboardFilterDTO)
  {
    return dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(dashboardFilterDTO);
  }

  /**
   * @since 1.24.0
   */
  @POST
  @Path(DELETE_NAMED_FILTER_PATH)
  @ExceptionMetered(name = "deleteDashboardFiltersForCurrentUserByFilterNameExceptionMeter")
  @Audited(AuditEvent.DELETE_DASHBOARD_FILTER)
  public void deleteDashboardFilterForCurrentUserByFilterName(@QueryParam("filterName") final String filterName) {
    dashboardFilterService.deleteDashboardFilterForCurrentUserByFilterName(filterName);
  }

  /**
   * Export the violations as CSV. Use of FormDataMultiPart facilitates downloading results as file.
   *
   * @since 1.24.0
   */
  @POST
  @Path(GET_NEWEST_RISKS_EXPORT_PATH)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces("text/csv")
  @ExceptionMetered(name = "getNewestRisksExportExceptionMeter")
  @Audited(AuditEvent.EXPORT_DASHBOARD_VIOLATION_LIST)
  public Response getNewestRisksExport(@FormDataParam("filter") RisksFilterDTO risksFilterDTO) throws IOException {
    final List<NewestRiskDTO> results = newestRiskService
        .getNewestRisks(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds, risksFilterDTO.stageIds,
            risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories, risksFilterDTO.policyThreatLevelRange,
            risksFilterDTO.policyViolationStates, risksFilterDTO.orderBy, risksFilterDTO.maxDaysOld,
            0, Integer.MAX_VALUE).dashboardResults;

    String fileNamePrefix = calculateFileNamePrefixForView("violations");
    return Csv.generate(Response.ok(), fileNamePrefix, NewestRiskDTO.getCsvHeader(), results).build();
  }

  /**
   * Export the components as CSV. Use of FormDataMultiPart facilitates downloading results as file.
   *
   * @since 1.24.0
   */
  @POST
  @Path(GET_COMPONENT_RISKS_EXPORT_PATH)
  @Produces("text/csv")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @ExceptionMetered(name = "getComponentRisksExportExceptionMeter")
  @Audited(AuditEvent.EXPORT_DASHBOARD_COMPONENT_LIST)
  public Response getComponentRisksExport(@FormDataParam("filter") RisksFilterDTO risksFilterDTO) throws IOException {
    final List<ComponentRiskDTO> results = componentRiskService
        .getComponentRisks(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds, risksFilterDTO.stageIds,
            risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories, risksFilterDTO.policyThreatLevelRange,
            risksFilterDTO.policyViolationStates, risksFilterDTO.orderBy, Integer.MAX_VALUE).dashboardResults;

    String fileNamePrefix = calculateFileNamePrefixForView("components");
    return Csv.generate(Response.ok(), fileNamePrefix, ComponentRiskDTO.getCsvHeader(), results).build();
  }

  /**
   * Export the applications as CSV. Use of FormDataMultiPart facilitates downloading results as file.
   *
   * @since 1.24.0
   */
  @POST
  @Path(GET_APPLICATION_RISKS_EXPORT_PATH)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces("text/csv")
  @ExceptionMetered(name = "getApplicationRisksExportExceptionMeter")
  @Audited(AuditEvent.EXPORT_DASHBOARD_APPLICATION_LIST)
  public Response getApplicationRisksExport(@FormDataParam("filter") RisksFilterDTO risksFilterDTO) throws IOException {
    final List<ApplicationRiskScoreDTO> results = applicationRiskService
        .getApplicationRisks(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds,
            risksFilterDTO.stageIds, risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories,
            risksFilterDTO.policyThreatLevelRange, risksFilterDTO.policyViolationStates, risksFilterDTO.orderBy,
            Integer.MAX_VALUE).dashboardResults;

    String fileNamePrefix = calculateFileNamePrefixForView("applications");
    return Csv.generate(Response.ok(), fileNamePrefix, ApplicationRiskScoreDTO.getCsvHeader(), results).build();
  }

  /**
   * @since 1.147
   */
  @POST
  @Path(GET_POLICY_WAIVERS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.VIEW_DASHBOARD_WAIVER_LIST)
  @ExceptionMetered(name = "getPolicyWaiversExceptionMeter")
  public DashboardResultsDTO<DashboardPolicyWaiverDTO> getPolicyWaivers(RisksFilterDTO risksFilterDTO) {
    if (risksFilterDTO == null) {
      throw new BadRequestException("Invalid filter supplied for request.");
    }
    return dashboardPolicyWaiverService.getDashboardPolicyWaivers(risksFilterDTO);
  }

  /**
   * @since 1.147
   */
  @POST
  @Path(GET_POLICY_WAIVERS_EXPORT_PATH)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces("text/csv")
  @ExceptionMetered(name = "getPolicyWaiversExportExceptionMeter")
  @Audited(AuditEvent.EXPORT_DASHBOARD_WAIVER_LIST)
  public Response getPolicyWaiversExport(@FormDataParam("filter") RisksFilterDTO risksFilterDTO) throws IOException {
    risksFilterDTO.maxResults = Integer.MAX_VALUE;

    final List<DashboardPolicyWaiverDTO> results = dashboardPolicyWaiverService
        .getDashboardPolicyWaiversForExport(risksFilterDTO).dashboardResults;

    String fileNamePrefix = calculateFileNamePrefixForView("waivers");
    return Csv.generate(Response.ok(), fileNamePrefix, DashboardPolicyWaiverDTO.getCsvHeader(), results).build();
  }

  private String calculateFileNamePrefixForView(final String viewName) throws IOException {
    NamedDashboardFilterDTO activeFilter = dashboardFilterService.getActiveDashboardFilterForCurrentUser();
    NamedDashboardFilterDTO basedOnFilterForActiveFilter = null;
    if (activeFilter.basedOnFilterName != null) {
      List<NamedDashboardFilterDTO> allFiltersForCurrentUser =
          dashboardFilterService.getNamedDashboardFiltersForCurrentUser();

      for (final NamedDashboardFilterDTO namedDashboardFilterDTO : allFiltersForCurrentUser) {
        if (namedDashboardFilterDTO.name.equals(activeFilter.basedOnFilterName)) {
          basedOnFilterForActiveFilter = namedDashboardFilterDTO;
          break;
        }
      }
    }

    String fileNamePrefix;
    if (basedOnFilterForActiveFilter != null && activeFilter.filter.equals(basedOnFilterForActiveFilter.filter)) {
      fileNamePrefix = activeFilter.basedOnFilterName.replace(" ", "_");
    }
    else {
      fileNamePrefix = "results";
    }
    return fileNamePrefix + "-" + viewName;
  }
}
