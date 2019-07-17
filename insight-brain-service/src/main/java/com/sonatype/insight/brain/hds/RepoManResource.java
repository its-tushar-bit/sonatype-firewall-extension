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
import com.sonatype.insight.brain.telemetry.UserTelemetryResource;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ClientScanType;

import com.codahale.metrics.annotation.Timed;

@Path(RepoManResource.RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.RM_STAGING_INTEGRATION)
@Named
@Timed
public class RepoManResource
{
  public static final String RESOURCE_PATH = "rest/rm";

  public static final String SCAN_PATH = "scan/{applicationPublicId}";

  private final ScanHandler scanHandler;

  private final UserTelemetryResource userTelemetryResource;

  @Inject
  public RepoManResource(final ScanHandler scanHandler, final UserTelemetryResource userTelemetryResource) {
    this.scanHandler = scanHandler;
    this.userTelemetryResource = userTelemetryResource;
  }

  @PUT
  @Path(SCAN_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ScanReceipt uploadScan(@PathParam("applicationPublicId") final String applicationPublicId,
                                @Context HttpServletRequest req) throws IOException
  {
    return scanHandler.handle(req, applicationPublicId, ClientScanType.SONATYPE);
  }

  /**
   * Expose all user-telemetry endpoints here as well so that they can be reached from the version-graph when
   * running in NXRM. NXRM has a whitelist of resource paths that it will allow to be proxied to IQ, which includes
   * anything under /rest/rm
   */
  @Path(UserTelemetryResource.RESOURCE_SUBPATH)
  public UserTelemetryResource proxyTelemetry() {
    return userTelemetryResource;
  }
}
