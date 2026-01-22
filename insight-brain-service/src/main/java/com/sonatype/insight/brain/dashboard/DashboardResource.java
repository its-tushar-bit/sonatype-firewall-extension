/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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

  public static final String GET_VIOLATION_RISKS_PATH = "policy/newestRisks";

  public static final String GET_VIOLATION_RISKS_EXPORT_PATH = "export/newestRisks";

  public static final String GET_COMPONENT_RISKS_PATH = "policy/componentRisks";

  public static final String GET_COMPONENT_RISKS_EXPORT_PATH = "export/componentRisks";

  public static final String GET_APPLICATION_RISKS_PATH = "policy/applicationRisks";

  public static final String GET_APPLICATION_RISKS_EXPORT_PATH = "export/applicationRisks";

  public static final String GET_POLICY_WAIVERS_PATH = "policy/policyWaivers";

  public static final String GET_POLICY_WAIVERS_EXPORT_PATH = "export/policyWaivers";

  static final String GET_POLICY_WAIVER_REQUESTS_PATH = "policy/policyWaiverRequests";

  static final String GET_POLICY_WAIVER_REQUESTS_EXPORT_PATH = "export/policyWaiverRequests";

  public static final String FILTERS_PATH = "filters/active";

  public static final String NAMED_FILTERS_PATH = "filters/named";

  public static final String DELETE_NAMED_FILTER_PATH = NAMED_FILTERS_PATH + "/delete";

  private final ApplicationRiskService applicationRiskService;

  private final DashboardComponentRiskService componentRiskService;

  private final DashboardFilterService dashboardFilterService;

  private final DashboardViolationRiskService dashboardViolationRiskService;

  private final PolicyWaiverService policyWaiverService;

  private final DashboardPolicyWaiverRequestService dashboardPolicyWaiverRequestService;

  @Inject
  public DashboardResource(
      ApplicationRiskService applicationRiskService,
      DashboardFilterService dashboardFilterService,
      DashboardComponentRiskService componentRiskService,
      DashboardViolationRiskService dashboardViolationRiskService,
      PolicyWaiverService policyWaiverService,
      DashboardPolicyWaiverRequestService dashboardPolicyWaiverRequestService)
  {
    this.applicationRiskService = applicationRiskService;
    this.componentRiskService = componentRiskService;
    this.dashboardFilterService = dashboardFilterService;
    this.dashboardViolationRiskService = dashboardViolationRiskService;
    this.policyWaiverService = policyWaiverService;
    this.dashboardPolicyWaiverRequestService = dashboardPolicyWaiverRequestService;
  }

  @POST
  @Path(GET_VIOLATION_RISKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getViolationRiskServiceRisksExceptionMeter")
  @Audited(AuditEvent.VIEW_DASHBOARD_VIOLATION_LIST)
  public DashboardResultsDTO<DashboardViolationRiskDTO> getViolationRisks(RisksFilterDTO risksFilterDTO) {
    return dashboardViolationRiskService.get(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds,
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
        risksFilterDTO.page, risksFilterDTO.pageSize);
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
        risksFilterDTO.page, risksFilterDTO.pageSize);
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
  @Path(GET_VIOLATION_RISKS_EXPORT_PATH)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces("text/csv")
  @ExceptionMetered(name = "getViolationRisksExportExceptionMeter")
  @Audited(AuditEvent.EXPORT_DASHBOARD_VIOLATION_LIST)
  public Response getViolationRisksExport(@FormDataParam("filter") RisksFilterDTO risksFilterDTO) throws IOException {
    final List<DashboardViolationRiskDTO> results = dashboardViolationRiskService
        .get(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds, risksFilterDTO.stageIds,
            risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories, risksFilterDTO.policyThreatLevelRange,
            risksFilterDTO.policyViolationStates, risksFilterDTO.orderBy, risksFilterDTO.maxDaysOld,
            0, Integer.MAX_VALUE).dashboardResults;

    String fileNamePrefix = calculateFileNamePrefixForView("violations");
    return Csv.generate(Response.ok(), fileNamePrefix, DashboardViolationRiskDTO.getCsvHeader(), results).build();
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
            risksFilterDTO.policyViolationStates, risksFilterDTO.orderBy, 0, Integer.MAX_VALUE).dashboardResults;

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
            0, Integer.MAX_VALUE).dashboardResults;

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
  public DashboardResultsDTO<DashboardPolicyWaiverDTO> getPolicyWaivers(
      RisksFilterDTO risksFilterDTO,
      @QueryParam("includeAutoWaivers") @DefaultValue("true") boolean includeAutoWaivers)
  {
    if (risksFilterDTO == null) {
      throw new BadRequestException("Invalid filter supplied for request.");
    }
    return policyWaiverService.getDashboardPolicyWaivers(risksFilterDTO, includeAutoWaivers);
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
  public Response getPolicyWaiversExport(
      @FormDataParam("filter") RisksFilterDTO risksFilterDTO,
      @QueryParam("includeAutoWaivers") @DefaultValue("true") boolean includeAutoWaivers) throws IOException
  {
    risksFilterDTO.pageSize = Integer.MAX_VALUE;
    risksFilterDTO.page = 0;

    final List<DashboardPolicyWaiverDTO> results = policyWaiverService
        .getDashboardPolicyWaiversForExport(risksFilterDTO, includeAutoWaivers).dashboardResults;

    String fileNamePrefix = calculateFileNamePrefixForView("waivers");
    return Csv.generate(Response.ok(), fileNamePrefix, DashboardPolicyWaiverDTO.getCsvHeader(), results).build();
  }

  @POST
  @Path(GET_POLICY_WAIVER_REQUESTS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered(name = "getPolicyWaiverRequestsExceptionMeter")
  public DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> getPolicyWaiverRequests(RisksFilterDTO risksFilterDTO) {
    return dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequests(risksFilterDTO);
  }

  @POST
  @Path(GET_POLICY_WAIVER_REQUESTS_EXPORT_PATH)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces("text/csv")
  @ExceptionMetered(name = "getPolicyWaiverRequestsExportExceptionMeter")
  public Response getPolicyWaiverRequestsExport(
      @FormDataParam("filter") RisksFilterDTO risksFilterDTO) throws IOException
  {
    risksFilterDTO.pageSize = Integer.MAX_VALUE;
    risksFilterDTO.page = 0;

    List<DashboardPolicyWaiverRequestDTO> results =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequestsForExport(risksFilterDTO).dashboardResults;

    String fileNamePrefix = calculateFileNamePrefixForView("waiver-requests");
    return Csv.generate(Response.ok(), fileNamePrefix, DashboardPolicyWaiverRequestDTO.getCsvHeader(), results).build();
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
