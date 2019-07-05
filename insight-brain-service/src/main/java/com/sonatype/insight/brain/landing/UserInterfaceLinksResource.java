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

import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.UnlicensedPath;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.codahale.metrics.annotation.Timed;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.apache.shiro.SecurityUtils;

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
@Path(UserInterfaceLinksResource.RESOURCE_PATH)
@UnlicensedPath
public class UserInterfaceLinksResource
{
  public static final String RESOURCE_PATH = "ui/links";

  public static final String MANAGEMENT_PATH = "{ownerType: application|organization}/{ownerId}/management";

  public static final String REPORT_PATH = "application/{applicationPublicId}/report/{scanId}";

  public static final String EMBEDDABLE_REPORT_PATH = "application/{applicationPublicId}/report/{scanId}/embeddable";

  public static final String PDF_PATH = "application/{applicationPublicId}/report/{scanId}/pdf";

  public static final String REPO_RESULT_PATH = "repository/{repositoryId}/result";

  public static final String VULNERABILITY_DETAILS_PATH = "vln/{vulnerabilityId}";

  private final BaseUrl baseUrl;

  private final TelemetrySender telemetrySender;

  @Inject
  public UserInterfaceLinksResource(BaseUrl baseUrl, TelemetrySender telemetrySender) {
    this.baseUrl = baseUrl;
    this.telemetrySender = telemetrySender;
  }

  private Response redirect(UriBuilder uriBuilder, Object... parameters) {
    URI uri = uriBuilder.build(parameters);
    uri = URI.create(uri.toString().replaceAll("%2F", "/"));
    uri = URI.create(uri.toString().replaceAll("%3F", "?"));
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
  @Path(REPORT_PATH)
  public Response linkToReport(@PathParam("applicationPublicId") String applicationPublicId,
                               @PathParam("scanId") String scanId,
                               @QueryParam("source") String source)
  {
    sendSourceTelemetryData(applicationPublicId, scanId, source);
    return linkToReport(applicationPublicId, scanId, false);
  }

  /**
   * @since 1.16
   */
  @GET
  @Path(EMBEDDABLE_REPORT_PATH)
  public Response linkToEmbeddableReport(@PathParam("applicationPublicId") String applicationPublicId,
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
  public Response linkToPdf(@PathParam("applicationPublicId") String applicationPublicId,
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

  @VisibleForTesting
  void sendSourceTelemetryData(final String applicationId, final String scanId, final String source) {
    if (source == null) {
      return;
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.SOURCE_CONTROL_REPORT_LINK);
    telemetryData.setAttributes(ImmutableMap
        .of("source", source.toLowerCase(Locale.ENGLISH), "applicationId", HdsClientAnalytics.obfuscate(applicationId),
            "scanId", HdsClientAnalytics.obfuscate(scanId), "isLoggedIn",
            SecurityUtils.getSubject().getPrincipal() != null));
    telemetrySender.send(telemetryData);
  }

  private static String buildStableUrl(String path, Object... parameters) {
    return UriBuilder.fromPath(UserInterfaceLinksResource.RESOURCE_PATH).path(path).build(parameters).toString();
  }

  /**
   * Gets the relative URL to the stable hyperlink for the HTML report of the given application and scan.
   */
  public static String getReportUrl(String applicationPublicId, String scanId) {
    return buildStableUrl(UserInterfaceLinksResource.REPORT_PATH, applicationPublicId, scanId);
  }

  /**
   * Gets the relative URL to the stable hyperlink for the embeddable HTML report of the given application and scan.
   * 
   * @since 1.16
   */
  public static String getEmbeddableReportUrl(String applicationPublicId, String scanId) {
    return buildStableUrl(UserInterfaceLinksResource.EMBEDDABLE_REPORT_PATH, applicationPublicId, scanId);
  }

  /**
   * Gets the relative URL to the stable hyperlink for the PDF report of the given application and scan.
   * 
   * @since 1.9
   */
  public static String getPdfUrl(String applicationPublicId, String scanId) {
    return buildStableUrl(UserInterfaceLinksResource.PDF_PATH, applicationPublicId, scanId);
  }

  /**
   * Gets the relative URL to the stable hyperlink for the repository audit report for a given rm/repository
   *
   * @since 1.17
   */
  public static String getRepositoryReportUrl(String repositoryId) {
    return buildStableUrl(REPO_RESULT_PATH, repositoryId);
  }
}
