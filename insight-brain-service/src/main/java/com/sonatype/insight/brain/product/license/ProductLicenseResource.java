/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.product.license.CLMLicenseManager.LicenseSummary;
import com.sonatype.insight.brain.security.AntiCsrfFilter;

import com.sun.jersey.multipart.FormDataParam;

@Path(ProductLicenseResource.SERVICE_PATH)
@Named
public class ProductLicenseResource
{
  public static final String SERVICE_PATH = "rest/product/license";

  private final ProductLicenseService productLicenseService;

  private final AntiCsrfFilter antiCsrfFilter;

  @Inject
  public ProductLicenseResource(ProductLicenseService productLicenseService, AntiCsrfFilter antiCsrfFilter) {
    this.productLicenseService = productLicenseService;
    this.antiCsrfFilter = antiCsrfFilter;
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_PLAIN)
  @UnlicensedPath
  public String installLicense(@FormDataParam("file") InputStream is,
      @FormDataParam(AntiCsrfFilter.CSRF_HEADER_NAME) String csrfToken, @Context HttpHeaders headers,
      @QueryParam("forceSuccess") boolean forceSuccess)
  {
    try {
      antiCsrfFilter.validate(csrfToken, headers);
      productLicenseService.installLicense(is);
      // Note an empty string triggers success in the UI
      return "";
    }
    catch (Exception e) {
      // IE<10 will only work in case of a 200 response, otherwise the response gets junked and replaced with some local
      // error page which then fails to load because of cross site scripting probs
      if (forceSuccess) {
        return e.getMessage();
      }
      throw e;
    }
  }

  @DELETE
  public void uninstallLicense() throws Exception {
    productLicenseService.uninstallLicense();
  }

  @GET
  @UnlicensedPath
  @Produces(MediaType.APPLICATION_JSON)
  public LicenseSummary validate() {
    return productLicenseService.validateLicense();
  }
}
