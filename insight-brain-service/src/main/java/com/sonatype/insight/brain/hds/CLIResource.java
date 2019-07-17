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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.model.ClientScanType;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.19.0
 */
@Path(CLIResource.RESOURCE_PATH)
@Named
@Timed
public class CLIResource
{
  public static final String RESOURCE_PATH = "rest/cli";

  public static final String SCAN_PATH = "scan/{applicationPublicId}";

  private final ScanHandler scanHandler;

  private final ProductLicense productLicense;

  @Inject
  public CLIResource(ScanHandler scanHandler, ProductLicense productLicense) {
    this.scanHandler = scanHandler;
    this.productLicense = productLicense;
  }

  /**
   * Used to upload a scan from the CLI scanner.
   * 
   * @param clientScanType null if the CLI scanner that uploads the scan is 1.23 or earlier.
   */
  @PUT
  @Path(SCAN_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public ScanReceipt putScan(@PathParam("applicationPublicId") final String applicationPublicId,
                             @QueryParam("scanType") ClientScanType clientScanType,
                             @Context HttpServletRequest req) throws IOException
  {
    productLicense.validateFeature(LicensedFeature.CLI_INTEGRATION);
    return scanHandler.handle(req, applicationPublicId, clientScanType);
  }
}
