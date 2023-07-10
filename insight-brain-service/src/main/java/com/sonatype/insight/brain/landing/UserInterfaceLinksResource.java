/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import java.net.URI;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.product.license.UnlicensedPath;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;

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
  private final BaseUrl baseUrl;

  private final TelemetrySender telemetrySender;

  private final CurrentUser currentUser;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  public UserInterfaceLinksResource(
      BaseUrl baseUrl,
      TelemetrySender telemetrySender,
      CurrentUser currentUser,
      ApplicationDAO applicationDAO,
      PolicyEvaluationDAO policyEvaluationDAO)
  {
    this.baseUrl = baseUrl;
    this.telemetrySender = telemetrySender;
    this.currentUser = currentUser;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
  }

  private Response redirect(UriBuilder uriBuilder, Object... parameters) {
    URI uri = uriBuilder.build(parameters);
    uri = URI.create(uri.toString().replaceAll("%2F", "/").replaceAll("%3F", "?"));
    return Response.temporaryRedirect(uri).build();
  }

  @GET
  @Path(MANAGEMENT_PATH)
  public Response linkToManagement(@PathParam("ownerType") OwnerType ownerType, @PathParam("ownerId") String ownerId) {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(InsightBrainService.BRAIN_ASSET_PATH + "index.html")
        .fragment("/management/view/{ownerType}/{ownerId}");
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
    uriBuilder.path(InsightBrainService.BRAIN_ASSET_PATH + "index.html")
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
    sendSourceTelemetryData(application != null ? application.getId() : applicationPublicId, scanId, source);
    return linkToReport(applicationPublicId, scanId, false);
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
    uriBuilder.path(InsightBrainService.BRAIN_ASSET_PATH + "index.html").fragment(fragmentTemplate);

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
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(InsightBrainService.BRAIN_ASSET_PATH + "index.html").fragment("/" + REPO_RESULT_PATH);
    return redirect(uriBuilder, repositoryId);
  }

  @GET
  @Path(VULNERABILITY_DETAILS_PATH)
  public Response linkToVulnerabilityDetails(@PathParam("vulnerabilityId") String vulnerabilityId) {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(InsightBrainService.BRAIN_ASSET_PATH + "index.html").fragment("/vulnerabilities/{vulnerabilityId}");
    return redirect(uriBuilder, vulnerabilityId);
  }

  @GET
  @Path(LATEST_VERSION_SBOM_REPORT_PATH)
  public Response linkToSbom(@PathParam("applicationId") String applicationId, @PathParam("scanId") String scanId) {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(PublicApiPaths.CYCLONE_DX_RESOURCE_PATH).path("1.4/{applicationId}/reports/{reportId}");
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
    uriBuilder.path(InsightBrainService.BRAIN_ASSET_PATH + "index.html").fragment("/" +
        QUARANTINED_COMPONENT_REPORT_PATH);
    return redirect(uriBuilder, token);
  }

  private void sendSourceTelemetryData(final String applicationId, final String scanId, final String source) {
    if (source == null) {
      return;
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL_REPORT_LINK);
    telemetryData.setAttributes(ImmutableMap
        .of("source", source.toLowerCase(Locale.ENGLISH),
            "application_id", HdsClientAnalytics.obfuscate(applicationId),
            "scan_id", HdsClientAnalytics.obfuscate(scanId),
            "is_logged_in", !currentUser.isAnonymous()));
    telemetrySender.send(telemetryData);
  }
}
