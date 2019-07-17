/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ClientScanType;

import com.codahale.metrics.annotation.Timed;

@Path(CIResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.CI_INTEGRATION)
@Named
@Timed
public class CIResource
{
  public static final String RESOURCE_PATH = "rest/ci";

  public static final String SCAN_PATH = "scan/{applicationPublicId}";

  private final ScanHandler scanHandler;

  @Inject
  public CIResource(final ScanHandler scanHandler) {
    this.scanHandler = scanHandler;
  }

  @PUT
  @Path(SCAN_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ScanReceipt putScan(@PathParam("applicationPublicId") final String applicationPublicId,
                             @Context HttpServletRequest req) throws IOException
  {
    return scanHandler.handle(req, applicationPublicId, ClientScanType.SONATYPE);
  }
}
