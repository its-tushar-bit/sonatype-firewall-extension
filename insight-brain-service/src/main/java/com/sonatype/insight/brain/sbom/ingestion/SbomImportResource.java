/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.io.InputStream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Named
@Timed
@Singleton
@Path(SbomImportResource.RESOURCE_PATH)
public class SbomImportResource
{
  public static final String RESOURCE_PATH = "rest/sbom";

  public static final String DETECT_PATH = "/detect/{applicationId}";

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
  public SbomDetectionResultDTO detectSbom(@PathParam("applicationId") String applicationId,
                                           @FormDataParam("file") InputStream sbom)
  {
    return sbomImportService.detectSbom(applicationId, sbom);
  }
}
