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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.yammer.metrics.annotation.ExceptionMetered;
import com.yammer.metrics.annotation.Timed;

@Named
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

  public static final String GET_POLICY_SUMMARY_PATH = "policy/summary";

  public static final String FILTERS_PATH = "filters/active";

  public static final String NAMED_FILTERS_PATH = "filters/named";
  
  public static final String FILTERS_SUMMARY_PATH = "filters/summary";

  private final ApplicationRiskService applicationRiskService;

  private final ComponentRiskService componentRiskService;

  private final DashboardFilterService dashboardFilterService;

  private final NewestRiskService newestRiskService;

  private final PolicySummaryService policySummaryService;

  @Inject
  public DashboardResource(ApplicationRiskService applicationRiskService,
                           DashboardFilterService dashboardFilterService,
                           ComponentRiskService componentRiskService,
                           NewestRiskService newestRiskService,
                           PolicySummaryService policySummaryService)
  {
    this.applicationRiskService = applicationRiskService;
    this.componentRiskService = componentRiskService;
    this.dashboardFilterService = dashboardFilterService;
    this.newestRiskService = newestRiskService;
    this.policySummaryService = policySummaryService;
  }

  @POST
  @Path(GET_NEWEST_RISKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getNewestRisksExceptionMeter")
  public List<NewestRiskDTO> getNewestRisks(RisksFilterDTO risksFilterDTO) {
    return newestRiskService.getNewestRisks(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds,
        risksFilterDTO.stageIds, risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories,
        risksFilterDTO.policyThreatLevelRange, risksFilterDTO.maxResults);
  }

  @POST
  @Path(GET_APPLICATION_RISKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getApplicationRisksExceptionMeter")
  public List<ApplicationRiskScoreDTO> getApplicationRisks(RisksFilterDTO risksFilterDTO) {
    return applicationRiskService.getApplicationRisks(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds,
        risksFilterDTO.stageIds, risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories,
        risksFilterDTO.policyThreatLevelRange, risksFilterDTO.maxResults);
  }

  @POST
  @Path(GET_COMPONENT_RISKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getComponentRisksExceptionMeter")
  public List<ComponentRiskDTO> getComponentRisks(RisksFilterDTO risksFilterDTO) {
    return componentRiskService.getComponentRisks(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds,
        risksFilterDTO.stageIds, risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories,
        risksFilterDTO.policyThreatLevelRange, risksFilterDTO.maxResults);
  }

  /**
   * @since 1.11.0
   */
  @GET
  @Path(FILTERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getActiveDashboardFilterForCurrentUserExceptionMeter")
  public DashboardFilterDTO getActiveDashboardFilterForCurrentUser() throws IOException {
    return dashboardFilterService.getActiveDashboardFilterForCurrentUser();
  }

  /**
   * @since 1.24.0
   */
  @GET
  @Path(NAMED_FILTERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
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
  @Timed
  @ExceptionMetered(name = "updateUserDashboardFilterForCurrentUserExceptionMeter")
  public DashboardFilterDTO updateUserDashboardFilterForCurrentUser(DashboardFilterDTO dashboardFilterDTO) {
    NamedDashboardFilterDTO namedDashboardFilterDTO = new NamedDashboardFilterDTO();
    namedDashboardFilterDTO.name = "";
    namedDashboardFilterDTO.filter = dashboardFilterDTO;
    return dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(namedDashboardFilterDTO).filter;
  }

  /**
   * @since 1.24.0
   */
  @PUT
  @Path(NAMED_FILTERS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "createOrUpdateDashboardFilterForCurrentUserExceptionMeter")
  public NamedDashboardFilterDTO createOrUpdateDashboardFilterForCurrentUser(NamedDashboardFilterDTO dashboardFilterDTO) {
    return dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(dashboardFilterDTO);
  }
  
  /**
   * @since 1.24.0
   */
  @DELETE
  @Path(FILTERS_PATH)
  @Timed
  @ExceptionMetered(name = "deleteAllDashboardFiltersForCurrentUserExceptionMeter")
  public void deleteAllDashboardFiltersForCurrentUser() {
    dashboardFilterService.deleteAllDashboardFiltersForCurrentUser();
  }

  @POST
  @Path(FILTERS_SUMMARY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getFilterSummaryExceptionMeter")
  public FilterSummaryDTO getFilterSummary(RisksFilterDTO risksFilterDTO) {
    return dashboardFilterService.getFilterSummary(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds,
        risksFilterDTO.stageIds, risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories,
        risksFilterDTO.policyThreatLevelRange);
  }

  @POST
  @Path(GET_POLICY_SUMMARY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getPolicySummaryExceptionMeter")
  public PolicySummaryDTO getPolicySummary(RisksFilterDTO risksFilterDTO) {
    return policySummaryService.getPolicySummary(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds,
        risksFilterDTO.stageIds, risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories,
        risksFilterDTO.policyThreatLevelRange);
  }

  /**
   * Export the violations as CSV.
   * Use of FormDataMultiPart facilitates downloading results as file.
   * @since 1.24.0
   */
  @POST
  @Path(GET_NEWEST_RISKS_EXPORT_PATH)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces("text/csv")
  @Timed
  @ExceptionMetered(name = "getNewestRisksExportExceptionMeter")
  public Response getNewestRisksExport(FormDataMultiPart multiPart) throws IOException
  {
    String filterJson = multiPart.getField("filter").getValue();
    ObjectMapper mapper = new ObjectMapper();
    RisksFilterDTO risksFilterDTO = mapper.readValue(filterJson, RisksFilterDTO.class);
    final List<NewestRiskDTO> results = newestRiskService
        .getNewestRisks(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds, risksFilterDTO.stageIds,
            risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories, risksFilterDTO.policyThreatLevelRange,
            Integer.MAX_VALUE);
    return Csv.generate(Response.ok(), "results-violations", NewestRiskDTO.getCsvHeader(), results).build();
  }

  /**
   * Export the components as CSV.
   * Use of FormDataMultiPart facilitates downloading results as file.
   * @since 1.24.0
   */
  @POST
  @Path(GET_COMPONENT_RISKS_EXPORT_PATH)
  @Produces("text/csv")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Timed
  @ExceptionMetered(name = "getComponentRisksExportExceptionMeter")
  public Response getComponentRisksExport(FormDataMultiPart multiPart) throws IOException
  {
    String filterJson = multiPart.getField("filter").getValue();
    ObjectMapper mapper = new ObjectMapper();
    RisksFilterDTO risksFilterDTO = mapper.readValue(filterJson, RisksFilterDTO.class);
    final List<ComponentRiskDTO> results = componentRiskService
        .getComponentRisks(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds, risksFilterDTO.stageIds,
            risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories, risksFilterDTO.policyThreatLevelRange,
            Integer.MAX_VALUE);
    return Csv.generate(Response.ok(), "results-components", ComponentRiskDTO.getCsvHeader(), results).build();
  }

  /**
   * Export the applications as CSV.
   * Use of FormDataMultiPart facilitates downloading results as file.
   * @since 1.24.0
   */
  @POST
  @Path(GET_APPLICATION_RISKS_EXPORT_PATH)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces("text/csv")
  @Timed
  @ExceptionMetered(name = "getApplicationRisksExportExceptionMeter")
  public Response getApplicationRisksExport(FormDataMultiPart multiPart) throws IOException
  {
    String filterJson = multiPart.getField("filter").getValue();
    ObjectMapper mapper = new ObjectMapper();
    RisksFilterDTO risksFilterDTO = mapper.readValue(filterJson, RisksFilterDTO.class);
    final List<ApplicationRiskScoreDTO> results = applicationRiskService
        .getApplicationRisks(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds,
            risksFilterDTO.stageIds, risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories,
            risksFilterDTO.policyThreatLevelRange, Integer.MAX_VALUE);
    return Csv.generate(Response.ok(), "results-applications", ApplicationRiskScoreDTO.getCsvHeader(), results).build();
  }
}
