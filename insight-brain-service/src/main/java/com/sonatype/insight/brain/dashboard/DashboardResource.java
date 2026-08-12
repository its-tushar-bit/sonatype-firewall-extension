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
import com.sonatype.insight.brain.utils.CsvWritable;
import com.sonatype.insight.error.exception.BadRequestException;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shares the {@code rest/dashboard} class-level path with
 * {@link com.sonatype.insight.brain.dashboard.metrics.DashboardMetricsResource} (Nexus One metric
 * endpoints) — sub-paths must stay disjoint between the two classes.
 */
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

  private static final Logger log = LoggerFactory.getLogger(DashboardResource.class);

  /**
   * CLM-39953: system property that caps the number of rows a single dashboard CSV export may
   * materialise. See {@link #maxExportRows()}.
   */
  static final String MAX_EXPORT_ROWS_PROPERTY = "com.sonatype.insight.brain.dashboard.export.maxRows";

  static final int DEFAULT_MAX_EXPORT_ROWS = 500_000;

  /**
   * Response header set to {@code true} when a CSV export was capped at {@link #maxExportRows()}.
   *
   * <p>
   * Best-effort machine signal only: the dashboard downloads exports via a native
   * {@code <form method="post">} submit, so browser JS cannot read this header. The user-facing
   * truncation signal is the in-band {@code #}-comment row appended to the CSV body (see
   * {@link #truncationNotice(int)}); this header remains useful for API/scripted consumers.
   */
  static final String EXPORT_TRUNCATED_HEADER = "X-Sonatype-Export-Truncated";

  /**
   * Response header carrying the active row cap when an export is truncated. Best-effort; see
   * {@link #EXPORT_TRUNCATED_HEADER}.
   */
  static final String EXPORT_ROW_LIMIT_HEADER = "X-Sonatype-Export-Row-Limit";

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

  /**
   * CLM-39953: dashboard CSV exports previously passed {@link Integer#MAX_VALUE} as the page size.
   * The DAOs treat that as an "unlimited" sentinel that skips the SQL {@code LIMIT} clause and
   * materialises the entire result set into heap, so at enterprise scale (tens of thousands of
   * applications, millions of violations) a single export exhausts the JVM heap and triggers a Full
   * GC spiral / OOM.
   *
   * <p>
   * <b>Where the cap bounds the fetch (heap-protected):</b> the violations, components and
   * (on Postgres) applications exports, plus the Firewall text-filtered waivers export, thread this
   * value straight into a DAO query, whose {@code pageSize < Integer.MAX_VALUE} guard then applies
   * {@code LIMIT} — so at most this many rows are ever loaded. This is the confirmed OOM path from
   * the ticket.
   *
   * <p>
   * <b>Where the cap only bounds the output (not yet the fetch):</b> the default (no-text-filter)
   * policy-waivers export and the policy-waiver-requests export still materialise the full filtered
   * set in memory and then emit only the first {@code maxExportRows()} rows via
   * {@link com.google.common.collect.Lists#partition}. For those two paths this cap curbs the CSV
   * size and the downstream cost, but does not (yet) bound the in-heap fetch; pushing the limit into
   * those DAO queries is tracked as follow-up. Either way the response is flagged as truncated so it
   * is never silently short.
   *
   * <p>
   * Configurable JVM-wide via the {@value #MAX_EXPORT_ROWS_PROPERTY} system property (default
   * {@value #DEFAULT_MAX_EXPORT_ROWS}); non-positive, unparseable, or {@link Integer#MAX_VALUE}
   * values fall back to the default. {@link Integer#MAX_VALUE} is explicitly rejected because it is
   * the very sentinel the DAOs read as "no {@code LIMIT}", which would reintroduce the OOM.
   */
  static int maxExportRows() {
    int configured = Integer.getInteger(MAX_EXPORT_ROWS_PROPERTY, DEFAULT_MAX_EXPORT_ROWS);
    return (configured >= 1 && configured < Integer.MAX_VALUE) ? configured : DEFAULT_MAX_EXPORT_ROWS;
  }

  /**
   * In-band, {@code #}-prefixed comment row appended to a truncated export so the downloaded file is
   * self-describing rather than silently short. Mirrors the convention already used by the streaming
   * list exports ({@code CsvExportLimits#TRUNCATION_NOTICE}); the {@code #} prefix keeps spreadsheet
   * users from mistaking it for data and lets scripted consumers skip it as a comment.
   */
  static String truncationNotice(int rowLimit) {
    return "# Export truncated at " + rowLimit + " rows; narrow the filters to export the remainder.";
  }

  /**
   * Finalises a dashboard CSV export. When the result set was capped (i.e. more rows matched the
   * filter than {@code rowCap}) the truncation is made observable three ways: a server-side WARN
   * log, best-effort response headers (for API consumers), and an in-band trailing {@code #}-comment
   * row so the downloaded file itself reveals it is incomplete.
   *
   * @param results the (already capped) export results; {@code hasNextPage} signals truncation
   * @param rowCap the row cap actually applied for this request (reused so the reported limit cannot
   *          drift from the applied one under a concurrent property change)
   * @param viewName human-readable export name, used only for the WARN log
   */
  private static Response buildExportResponse(
      DashboardResultsDTO<? extends CsvWritable> results,
      int rowCap,
      String fileNamePrefix,
      String header,
      String viewName)
  {
    Response.ResponseBuilder builder = Response.ok();
    String notice = null;
    if (results.hasNextPage) {
      log.warn("Dashboard '{}' CSV export truncated at the {}-row cap; more rows matched the filter. "
          + "Increase system property {} or narrow the export filter to retrieve the remainder.",
          viewName, rowCap, MAX_EXPORT_ROWS_PROPERTY);
      builder.header(EXPORT_TRUNCATED_HEADER, Boolean.TRUE.toString());
      builder.header(EXPORT_ROW_LIMIT_HEADER, rowCap);
      notice = truncationNotice(rowCap);
    }
    return Csv.generate(builder, fileNamePrefix, header, results.dashboardResults, notice).build();
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
    int rowCap = maxExportRows();
    final DashboardResultsDTO<DashboardViolationRiskDTO> exportResults = dashboardViolationRiskService
        .get(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds, risksFilterDTO.stageIds,
            risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories, risksFilterDTO.policyThreatLevelRange,
            risksFilterDTO.policyViolationStates, risksFilterDTO.orderBy, risksFilterDTO.maxDaysOld,
            0, rowCap);

    String fileNamePrefix = calculateFileNamePrefixForView("violations");
    return buildExportResponse(exportResults, rowCap, fileNamePrefix,
        DashboardViolationRiskDTO.getCsvHeader(), "violations");
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
    int rowCap = maxExportRows();
    final DashboardResultsDTO<ComponentRiskDTO> exportResults = componentRiskService
        .getComponentRisks(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds, risksFilterDTO.stageIds,
            risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories, risksFilterDTO.policyThreatLevelRange,
            risksFilterDTO.policyViolationStates, risksFilterDTO.orderBy, 0, rowCap);

    String fileNamePrefix = calculateFileNamePrefixForView("components");
    return buildExportResponse(exportResults, rowCap, fileNamePrefix,
        ComponentRiskDTO.getCsvHeader(), "components");
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
    int rowCap = maxExportRows();
    final DashboardResultsDTO<ApplicationRiskScoreDTO> exportResults = applicationRiskService
        .getApplicationRisks(risksFilterDTO.organizationIds, risksFilterDTO.applicationIds,
            risksFilterDTO.stageIds, risksFilterDTO.tagIds, risksFilterDTO.policyThreatCategories,
            risksFilterDTO.policyThreatLevelRange, risksFilterDTO.policyViolationStates, risksFilterDTO.orderBy,
            0, rowCap);

    String fileNamePrefix = calculateFileNamePrefixForView("applications");
    return buildExportResponse(exportResults, rowCap, fileNamePrefix,
        ApplicationRiskScoreDTO.getCsvHeader(), "applications");
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
    int rowCap = maxExportRows();
    risksFilterDTO.pageSize = rowCap;
    risksFilterDTO.page = 0;

    final DashboardResultsDTO<DashboardPolicyWaiverDTO> exportResults = policyWaiverService
        .getDashboardPolicyWaiversForExport(risksFilterDTO, includeAutoWaivers);

    String fileNamePrefix = calculateFileNamePrefixForView("waivers");
    return buildExportResponse(exportResults, rowCap, fileNamePrefix,
        DashboardPolicyWaiverDTO.getCsvHeader(), "waivers");
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
    int rowCap = maxExportRows();
    risksFilterDTO.pageSize = rowCap;
    risksFilterDTO.page = 0;

    DashboardResultsDTO<DashboardPolicyWaiverRequestDTO> exportResults =
        dashboardPolicyWaiverRequestService.getDashboardPolicyWaiverRequestsForExport(risksFilterDTO);

    String fileNamePrefix = calculateFileNamePrefixForView("waiver-requests");
    return buildExportResponse(exportResults, rowCap, fileNamePrefix,
        DashboardPolicyWaiverRequestDTO.getCsvHeader(), "waiver-requests");
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
