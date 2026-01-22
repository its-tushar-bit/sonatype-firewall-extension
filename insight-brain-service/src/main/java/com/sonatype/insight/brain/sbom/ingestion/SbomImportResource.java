/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.io.InputStream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Named
@Timed
@Singleton
@Path(SbomImportResource.RESOURCE_PATH)
public class SbomImportResource
{
  public static final String RESOURCE_PATH = "rest/sbom";

  public static final String DETECT_PATH = "/detect/{applicationId}";

  public static final String COMMIT_PATH = "/commit/{applicationId}/{applicationVersion}";

  private final SbomImportService sbomImportService;

  @Inject
  public SbomImportResource(final SbomImportService sbomImportService) {
    this.sbomImportService = sbomImportService;
  }

  @POST
  @Path(DETECT_PATH)
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Audited(AuditEvent.CREATE_SBOM_VERSION)
  public SbomDetectionResultDTO detectSbom(
      @PathParam("applicationId") String applicationId,
      @FormDataParam("file") InputStream sbom,
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @QueryParam("ignoreValidationError") boolean ignoreValidationError
  )
  {
    return sbomImportService.detectSbom(applicationId, sbom, fileDetail.getFileName(), ignoreValidationError);
  }

  @POST
  @Path(COMMIT_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  public Response importDetectedSbom(
      @PathParam("applicationId") String applicationId,

      // the app version that was detected and returned in the detectSbom response
      @PathParam("applicationVersion") String applicationVersion,

      // the version that the user wants to use instead of the detected one. Optional
      @QueryParam("applicationVersionOverride") String applicationVersionOverride,
      @Context HttpServletRequest req)
  {
    return sbomImportService.importDetectedSbom(
        applicationId,
        applicationVersion,
        applicationVersionOverride,
        HdsClient.getClientUserAgent(req)
    );
  }
}
