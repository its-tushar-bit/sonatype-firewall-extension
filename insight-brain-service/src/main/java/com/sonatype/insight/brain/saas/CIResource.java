/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

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
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

@Path(CIResource.SERVICE_PATH)
@ProductLicenseEnforcementPoint({ CLMEnforcementPoint.Build })
@Named
public class CIResource
{
  public static final String SERVICE_PATH = "rest/ci";

  private final ScanUploader uploader;

  @Inject
  public CIResource(final ScanUploader uploader) {
    this.uploader = uploader;
  }

  @PUT
  @Path("scan/{applicationPublicId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.EVALUATE_APPLICATION, anonymousAllowed = true)
  public ScanReceipt putScan(
      @PathParam("applicationPublicId") @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID)
      final String applicationPublicId,
      @Context HttpServletRequest req) throws IOException
  {
    return uploader.upload(req, applicationPublicId, "rest/ci/scan");
  }
}
