/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.license;

import java.io.InputStream;
import java.util.concurrent.Callable;

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
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.product.license.CLMLicenseManager.LicenseSummary;
import com.sonatype.insight.brain.security.AntiCsrfFilter;
import com.sonatype.insight.brain.utils.NgUploadResponseGenerator;

import com.sun.jersey.multipart.FormDataParam;

@Path(ProductLicenseResource.RESOURCE_PATH)
@Named
public class ProductLicenseResource
{
  public static final String RESOURCE_PATH = "rest/product/license";

  static final String VALIDATE_PATH = "validate";

  private final ProductLicenseService productLicenseService;

  private final NgUploadResponseGenerator ngUploadResponseGenerator;

  @Inject
  public ProductLicenseResource(ProductLicenseService productLicenseService,
                                NgUploadResponseGenerator ngUploadResponseGenerator)
  {
    this.productLicenseService = productLicenseService;
    this.ngUploadResponseGenerator = ngUploadResponseGenerator;
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_PLAIN)
  @UnlicensedPath
  public Response installLicense(@FormDataParam("file") final InputStream is,
                                 @FormDataParam(AntiCsrfFilter.CSRF_HEADER_NAME) String csrfToken,
                                 @Context HttpHeaders headers,
                                 @QueryParam("noFormData") boolean noFormData) throws Exception
  {
    return ngUploadResponseGenerator.run(csrfToken, headers, noFormData, new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        productLicenseService.installLicense(is);
        return null;
      }
    });
  }

  @DELETE
  public void uninstallLicense() throws Exception {
    productLicenseService.uninstallLicense();
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
  public LicenseSummary getLicenseSummary() {
    return productLicenseService.getLicenseSummary();
  }
}
