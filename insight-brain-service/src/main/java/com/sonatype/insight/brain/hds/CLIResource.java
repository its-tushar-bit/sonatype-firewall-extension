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
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;

/**
 * @since 1.19.0
 */
@Path(CLIResource.RESOURCE_PATH)
@Named
public class CLIResource
{
  public static final String RESOURCE_PATH = "rest/cli";

  public static final String SCAN_PATH = "scan/{applicationPublicId}";

  private final ScanUploader uploader;

  private CLMLicenseManager clmLicenseManager;

  @Inject
  public CLIResource(final ScanUploader uploader, CLMLicenseManager clmLicenseManager) {
    this.uploader = uploader;
    this.clmLicenseManager = clmLicenseManager;
  }

  @PUT
  @Path(SCAN_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.EVALUATE_APPLICATION, anonymousAllowed = true)
  public ScanReceipt putScan(
      @PathParam("applicationPublicId") @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID)
      final String applicationPublicId,
      @Context HttpServletRequest req) throws IOException
  {
    if (!clmLicenseManager.hasCLIScanning()) {
      throw new InvalidLicenseException();
    }
    return uploader.upload(req, applicationPublicId);
  }
}
