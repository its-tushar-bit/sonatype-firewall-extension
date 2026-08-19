/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ClientScanType;

import com.codahale.metrics.annotation.Timed;

/**
 * @deprecated Last plugin versions that use this REST resource:
 *             - Bamboo plugin: 1.12.1 (June 17, 2019)
 *             - Jenkins (aka Platform) plugin: 3.5.20190425-152158.c63841b (April 25, 2019)
 */
@Deprecated
@Path(DeprecatedCIResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.CI_INTEGRATION)
@Named
@Timed
public class DeprecatedCIResource
{
  public static final String RESOURCE_PATH = "rest/ci";

  public static final String SCAN_PATH = "scan/{applicationPublicId}";

  private final ScanHandler scanHandler;

  @Inject
  public DeprecatedCIResource(final ScanHandler scanHandler) {
    this.scanHandler = scanHandler;
  }

  @PUT
  @Path(SCAN_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ScanReceipt putScan(
      @PathParam("applicationPublicId") final String applicationPublicId,
      @Context HttpServletRequest req) throws IOException
  {
    return scanHandler.handle(req, applicationPublicId, ClientScanType.SONATYPE);
  }
}
