/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import java.net.URI;
import java.util.AbstractMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.product.license.UnlicensedPath;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.telemetry.TelemetryUtils;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.codahale.metrics.annotation.Timed;

import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.*;

/**
 * Provides URLs to parts of the UI for usage by enforcement points that wish to link to the CLM server's web interface.
 * Clients are expected to present hyperlinks to the REST resources here which when visited by the end user's browser
 * issue a redirect to the actual HTML page. The URLs to the REST resources are supposed to be more stable than the
 * direct URLs to the relevant HTML pages, thereby decoupling external clients from the specifics of the UI structure,
 * easing its evolution.
 *
 * @since 1.7
 */
@Named
@Timed
@Path(UserInterfaceLinksHelper.RESOURCE_PATH)
@UnlicensedPath
public class UserInterfaceLinksResource
{
  public static final String ASSET_INDEX_PATH = InsightBrainService.BRAIN_ASSET_PATH + "index.html";

  public static final String DEFAULT_CDX_BOM_SPECIFICATION = ExportSpecification.DEFAULT.getVersion();

  public static final String FIREWALL_REPOSITORY_RESULTS_PATH =
      "/firewall/repository/{repositoryId}/result";

  private final BaseUrl baseUrl;

  private final TelemetrySender telemetrySender;

  private final CurrentUser currentUser;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final TelemetryUtils telemetryUtils;

  private final RepositoryDAO repositoryDAO;

  @Inject
  public UserInterfaceLinksResource(
      BaseUrl baseUrl,
      TelemetrySender telemetrySender,
      CurrentUser currentUser,
      ApplicationDAO applicationDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      TelemetryUtils telemetryUtils,
      RepositoryDAO repositoryDAO)
  {
    this.baseUrl = baseUrl;
    this.telemetrySender = telemetrySender;
    this.currentUser = currentUser;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.telemetryUtils = telemetryUtils;
    this.repositoryDAO = repositoryDAO;
  }

  private Response redirect(UriBuilder uriBuilder, Object... parameters) {
    URI uri = uriBuilder.build(parameters);
    uri = URI.create(uri.toString().replaceAll("%2F", "/").replaceAll("%3F", "?"));
    return Response.temporaryRedirect(uri).build();
  }

  @GET
  @Path(DEVELOPER_HOME_PATH)
  public Response linkToDeveloperHome() {
    UriBuilder uriBuilder = baseUrl.redirect()
        .path(ASSET_INDEX_PATH)
        .fragment("/developer/dashboard");
    return redirect(uriBuilder);
  }

  @GET
  @Path(FIREWALL_HOME_PATH)
  public Response linkToFirewallHome() {
    UriBuilder uriBuilder = baseUrl.redirect()
        .path(ASSET_INDEX_PATH)
        .fragment("/firewall/dashboard");
    return redirect(uriBuilder);
  }

  @GET
  @Path(LIFECYCLE_HOME_PATH)
  public Response linkToLifecycleHome() {
    UriBuilder uriBuilder = baseUrl.redirect()
        .path(ASSET_INDEX_PATH)
        .fragment("/dashboard/violations");
    return redirect(uriBuilder);
  }

  @GET
  @Path(LIFECYCLE_ALT_HOME_PATH)
  public Response linkToLifecycleAltHome() {
    UriBuilder uriBuilder = baseUrl.redirect()
        .path(ASSET_INDEX_PATH)
        .fragment("/reports/violations");
    return redirect(uriBuilder);
  }

  @GET
  @Path(SBOM_MANAGER_HOME_PATH)
  public Response linkToSbomManagerHome() {
    UriBuilder uriBuilder = baseUrl.redirect()
        .path(ASSET_INDEX_PATH)
        .fragment("/sbomManager/dashboard");
    return redirect(uriBuilder);
  }

  @GET
  @Path(MANAGEMENT_PATH)
  public Response linkToManagement(@PathParam("ownerType") OwnerType ownerType, @PathParam("ownerId") String ownerId) {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH)
        .fragment("/management/view/{ownerType}/{ownerId}");
    return redirect(uriBuilder, ownerType, ownerId);
  }

  @GET
  @Path(SBOM_MANAGEMENT_PATH)
  public Response linkToSbomManagement(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId)
  {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH)
        .fragment("/sbomManager/management/view/{ownerType}/{ownerId}");
    return redirect(uriBuilder, ownerType, ownerId);
  }

  @GET
  @Path(SOURCE_CONTROL_MANAGEMENT_PATH)
  public Response linkToSourceControlManagement(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @QueryParam("githubAppSuccess") String githubAppSuccess)
  {
    UriBuilder uriBuilder = baseUrl.redirect();

    uriBuilder.replaceQuery("");

    String fragment = "/management/edit/{ownerType: application|organization}/{ownerId}/source-control";

    if ("true".equals(githubAppSuccess)) {
      fragment += "?githubAppSuccess=true";
    }

    uriBuilder.path(ASSET_INDEX_PATH).fragment(fragment);
    return redirect(uriBuilder, ownerType, ownerId);
  }

  @GET
  @Path(ITEM_MANAGEMENT_EDIT_PATH)
  public Response linkToItemManagementEdit(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @PathParam("itemType") String itemType,
      @PathParam("itemId") String itemId)
  {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH)
        .fragment("/management/edit/{ownerType: application|organization}/" +
            "{ownerId}/{itemType: category|label|policy}/{itemId}");
    return redirect(uriBuilder, ownerType, ownerId, itemType, itemId);
  }

  @GET
  @Path(LATEST_REPORT_PATH)
  public Response linkToLatestReport(
      @PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("stageId") String stageId)
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    PolicyEvaluation evaluation = policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(), stageId);
    if (evaluation == null) {
      throw new NotFoundException("The application " + applicationPublicId + " has no report at stage " + stageId);
    }
    return linkToReport(applicationPublicId, evaluation.getScanId(), false);
  }

  @GET
  @Path(REPORT_PATH)
  public Response linkToReport(
      @PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("scanId") String scanId,
      @QueryParam("source") String source)
  {
    Application application = applicationDAO.getByPublicId(applicationPublicId);
    sendSourceTelemetryData(application != null ? application.getId() : null, scanId, source);
    return linkToReport(applicationPublicId, scanId, false);
  }

  @GET
  @Path(COMPONENT_SCAN_REPORT_PATH)
  public Response linkToComponentScanReport(
      @PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("scanId") String scanId,
      @PathParam("componentScanHash") String componentScanHash,
      @QueryParam("source") String source,
      @QueryParam("tab") String tab)
  {
    Application application = applicationDAO.getByPublicId(applicationPublicId);
    sendSourceTelemetryData(application != null ? application.getId() : null, scanId, source);
    String fragmentTemplate = "/applicationReport/{applicationPublicId}/{scanId}/componentDetails/{componentScanHash}";
    String reportTab = getApplicationReportTab(tab);
    String fragmentTemplateWithTab = fragmentTemplate.concat("/").concat(reportTab);
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH).fragment(fragmentTemplateWithTab);
    return redirect(uriBuilder, applicationPublicId, scanId, componentScanHash);
  }

  @GET
  @Path(POLICY_VIOLATION_REPORT_PATH)
  public Response linkToPolicyViolationReport(
      @PathParam("policyViolationId") final String policyViolationId,
      @QueryParam("utm_source") final String utmSource)
  {
    final String fragmentTemplate = "/violation/{policyViolationId}?type=violation&sidebarReference=filter";
    final UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH).fragment(fragmentTemplate);
    return redirect(uriBuilder, policyViolationId, utmSource);
  }

  /**
   * @since 1.16
   */
  @GET
  @Path(EMBEDDABLE_REPORT_PATH)
  public Response linkToEmbeddableReport(
      @PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("scanId") String scanId)
  {
    return linkToReport(applicationPublicId, scanId, true);
  }

  private Response linkToReport(String applicationPublicId, String scanId, boolean embeddable) {
    String fragmentTemplate = "/applicationReport/{applicationPublicId}/{scanId}/policy" +
        (embeddable ? "?embeddable" : "");
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH).fragment(fragmentTemplate);

    return redirect(uriBuilder, applicationPublicId, scanId);
  }

  /**
   * @since 1.9
   */
  @GET
  @Path(PDF_PATH)
  public Response linkToPdf(
      @PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("scanId") String scanId)
  {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ReportResource.RESOURCE_PATH).path(ReportResource.PRINT_PATH);
    return redirect(uriBuilder, applicationPublicId, scanId);
  }

  @GET
  @Path(REPO_RESULT_PATH)
  public Response linkToRepositoryReport(@PathParam("repositoryId") String repositoryId) {
    Repository repository = repositoryDAO.getById(repositoryId);
    if (repository != null && "docker".equals(repository.getFormat())
        && repository.getRepositoryType() == RepositoryType.proxy)
    {
      UriBuilder uriBuilder = baseUrl.redirect();
      uriBuilder.path(ASSET_INDEX_PATH).fragment("/firewall/container/repository/{repositoryId}/results");
      return redirect(uriBuilder, repositoryId);
    }

    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH).fragment(FIREWALL_REPOSITORY_RESULTS_PATH);
    return redirect(uriBuilder, repositoryId);
  }

  @GET
  @Path(VULNERABILITY_DETAILS_PATH)
  public Response linkToVulnerabilityDetails(@PathParam("vulnerabilityId") String vulnerabilityId) {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH).fragment("/vulnerabilities/{vulnerabilityId}");
    return redirect(uriBuilder, vulnerabilityId);
  }

  @GET
  @Path(POLICY_VIOLATION_DETAILS_PATH)
  public Response linkToPolicyViolationDetails(@PathParam("violationId") String violationId) {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH).fragment("/violation/{violationId}");
    return redirect(uriBuilder, violationId);
  }

  @GET
  @Path(ADD_WAIVER_PATH)
  public Response linkToAddWaiver(
      @PathParam("violationId") String violationId,
      @QueryParam("comments") String comments,
      @QueryParam("reasonId") String reasonId)
  {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.replaceQueryParam("comments");
    uriBuilder.replaceQueryParam("reasonId");
    String fragment = "/addWaiver/{violationId}";
    fragment += comments != null ? "?comments={comments}" : "";
    fragment += reasonId != null ? "&reasonId={reasonId}" : "";
    uriBuilder.path(ASSET_INDEX_PATH).fragment(fragment);
    return redirect(uriBuilder, violationId, comments, reasonId);
  }

  @GET
  @Path(REVIEW_WAIVER_REQUEST_PATH)
  public Response linkToReviewWaiverRequest(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @PathParam("policyWaiverRequestId") String policyWaiverRequestId)
  {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH).fragment("/requestWaiverReview/{ownerType}/{ownerId}/{policyWaiverRequestId}");
    return redirect(uriBuilder, ownerType, ownerId, policyWaiverRequestId);
  }

  @GET
  @Path(LATEST_VERSION_SBOM_REPORT_PATH)
  public Response linkToSbom(@PathParam("applicationId") String applicationId, @PathParam("scanId") String scanId) {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(PublicApiPaths.CYCLONE_DX_RESOURCE_PATH)
        .path(DEFAULT_CDX_BOM_SPECIFICATION +
            "/{applicationId}/reports/{reportId}");
    return redirect(uriBuilder, applicationId, scanId);
  }

  @GET
  @Path(LATEST_VERSION_SPDX_REPORT_PATH)
  public Response linkToSpdx(@PathParam("applicationId") String applicationId, @PathParam("scanId") String scanId) {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(PublicApiPaths.SPDX_RESOURCE_PATH).path("/{applicationId}/reports/{reportId}");
    return redirect(uriBuilder, applicationId, scanId);
  }

  /**
   * @since 1.125
   */
  @GET
  @Path(QUARANTINED_COMPONENT_REPORT_PATH)
  public Response linkToQuarantinedComponentReport(@PathParam("token") String token) {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH)
        .fragment("/" +
            QUARANTINED_COMPONENT_REPORT_PATH);
    return redirect(uriBuilder, token);
  }

  @GET
  @Path(INTEGRATIONS_PRIORITIES_PATH)
  public Response linkToPrioritiesFromIntegrations(
      @PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("scanId") String scanId,
      @PathParam("integration") String integration)
  {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH).fragment(INTEGRATIONS_PRIORITIES_PATH);

    return redirect(uriBuilder, applicationPublicId, scanId, integration);
  }

  @GET
  @Path(PRIORITIES_PATH)
  public Response linkToPrioritiesReport(
      @PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("scanId") String scanId)
  {
    return linkToPrioritiesReportRedirect(applicationPublicId, scanId);
  }

  @GET
  @Path(PRIORITIES_PATH_LEGACY)
  public Response legacyLinkToPrioritiesReport(
      @PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("scanId") String scanId)
  {
    return linkToPrioritiesReportRedirect(applicationPublicId, scanId);
  }

  @GET
  @Path(SBOM_BOM_VIEW_PATH)
  public Response linkToSbomManagerBomPage(
      @PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("version") String version)
  {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH).fragment("/" + SBOM_BOM_VIEW_PATH + "/overview");
    return redirect(uriBuilder, applicationPublicId, version);
  }

  @GET
  @Path(ENTERPRISE_REPORTING_DASHBOARD_PATH)
  public Response linkToEnterpriseReportingDashboard(@PathParam("dashboardId") String dashboardId) {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH).fragment("/enterpriseReportingDashboard/{dashboardId}");
    return redirect(uriBuilder, dashboardId);
  }

  @GET
  @Path(FIREWALL_CONTAINER_IMAGE_EVALUATION_REPORT_PATH)
  public Response linkToMalwareDefenseContainerEvaluationReport(
      @PathParam("containerImagePublicId") String containerImagePublicId,
      @PathParam("scanId") String scanId)
  {
    String fragmentTemplate = "/firewall/containerReport/{applicationPublicId}/{scanId}/policy";
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH).fragment(fragmentTemplate);

    return redirect(uriBuilder, containerImagePublicId, scanId);
  }

  @GET
  @Path(MALWARE_DEFENSE_CONTAINER_IMAGE_EVALUATION_REPORT_PATH)
  public Response linkToMalwareDefenseContainerReportPolicy(
      @PathParam("containerImagePublicId") String containerImagePublicId,
      @PathParam("scanId") String scanId)
  {
    return linkToMalwareDefenseContainerEvaluationReport(containerImagePublicId, scanId);
  }

  @GET
  @Path(MALWARE_DEFENSE_REPOSITORY_RESULTS_PATH)
  public Response linkToMalwareDefenseRepositoryResults(@PathParam("repositoryId") String repositoryId) {
    return linkToRepositoryReport(repositoryId);
  }

  private Response linkToPrioritiesReportRedirect(final String applicationPublicId, final String scanId) {
    final UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(ASSET_INDEX_PATH).fragment(PRIORITIES_PATH);

    return redirect(uriBuilder, applicationPublicId, scanId);
  }

  private void sendSourceTelemetryData(final String applicationId, final String scanId, final String source) {
    if (source == null) {
      return;
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL_REPORT_LINK);

    Map<String, Object> telemetryAttributes = Stream.of(
        new AbstractMap.SimpleImmutableEntry<>("source", source.toLowerCase(Locale.ENGLISH)),
        new AbstractMap.SimpleImmutableEntry<>("application_id", telemetryUtils.obfuscate(applicationId)),
        new AbstractMap.SimpleImmutableEntry<>("scan_id", telemetryUtils.obfuscate(scanId)),
        new AbstractMap.SimpleImmutableEntry<>("is_logged_in", !currentUser.isAnonymous()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    telemetryUtils.includeRealApplicationId(telemetryAttributes, applicationId);
    telemetryData.setAttributes(telemetryAttributes);

    telemetrySender.send(telemetryData);
  }
}
