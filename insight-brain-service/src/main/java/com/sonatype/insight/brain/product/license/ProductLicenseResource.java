/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;

@Path(ProductLicenseResource.RESOURCE_PATH)
@Named
@Timed
public class ProductLicenseResource
{
  public static final String RESOURCE_PATH = "rest/product/license";

  static final String VALIDATE_PATH = "validate";

  private final ProductLicenseService productLicenseService;

  @Inject
  public ProductLicenseResource(ProductLicenseService productLicenseService) {
    this.productLicenseService = productLicenseService;
  }

  @GET
  @Path(VALIDATE_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @UnlicensedPath
  public LicenseSummary validateLicense() {
    return productLicenseService.validateLicense();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public LicenseInfo getLicenseInfo() {
    return productLicenseService.getLicenseInfo();
  }
}
