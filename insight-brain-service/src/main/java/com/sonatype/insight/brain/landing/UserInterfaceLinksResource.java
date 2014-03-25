/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.product.license.UnlicensedPath;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;

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
@Path(UserInterfaceLinksResource.SERVICE_PATH)
@UnlicensedPath
public class UserInterfaceLinksResource
{
  public static final String SERVICE_PATH = "ui/links";

  public static final String MANAGEMENT_PATH = "{ownerType: application|organization}/{ownerId}/management";

  public static final String REPORT_PATH = "application/{applicationPublicId}/report/{scanId}";

  public static final String PDF_PATH = "application/{applicationPublicId}/report/{scanId}/pdf";

  private final BaseUrl baseUrl;

  @Inject
  public UserInterfaceLinksResource(BaseUrl baseUrl) {
    this.baseUrl = baseUrl;
  }

  private Response redirect(URI uri) {
    return Response.temporaryRedirect(uri).build();
  }

  @GET
  @Path(MANAGEMENT_PATH)
  public Response linkToManagement(@PathParam("ownerType") String ownerType, @PathParam("ownerId") String ownerId) {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(InsightBrainService.BRAIN_ASSET_PATH + "index.html").fragment(
        "/management/{ownerType}/{ownerId}/policies");
    return redirect(uriBuilder.build(ownerType, ownerId));
  }

  @GET
  @Path(REPORT_PATH)
  public Response linkToReport(@PathParam("applicationPublicId") String applicationPublicId,
      @PathParam("scanId") String scanId)
  {
    UriBuilder uriBuilder = baseUrl.redirect();
    uriBuilder.path(InsightBrainService.BRAIN_ASSET_PATH + "index.html").fragment(
        "/reports/{applicationPublicId}/{scanId}");
    return redirect(uriBuilder.build(applicationPublicId, scanId));
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
    uriBuilder.path(ReportResource.SERVICE_PATH).path(ReportResource.PRINT_PATH);
    return redirect(uriBuilder.build(applicationPublicId, scanId));
  }

  /**
   * Gets the relative URL to the stable hyperlink for the HTML report of the given application and scan.
   */
  public static String getReportUrl(String applicationPublicId, String scanId) {
    return UriBuilder.fromPath(UserInterfaceLinksResource.SERVICE_PATH + '/' + UserInterfaceLinksResource.REPORT_PATH)
        .build(applicationPublicId, scanId).toString();
  }

  /**
   * Gets the relative URL to the stable hyperlink for the PDF report of the given application and scan.
   * 
   * @since 1.9
   */
  public static String getPdfUrl(String applicationPublicId, String scanId) {
    return UriBuilder.fromPath(UserInterfaceLinksResource.SERVICE_PATH + '/' + UserInterfaceLinksResource.PDF_PATH)
        .build(applicationPublicId, scanId).toString();
  }
}
