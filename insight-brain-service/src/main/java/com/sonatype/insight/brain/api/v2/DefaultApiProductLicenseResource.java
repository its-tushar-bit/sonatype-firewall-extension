/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseService;
import com.sonatype.insight.brain.product.license.UnlicensedPath;

import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * @since 1.114
 */
@Named
@Timed
@Path(PublicApiPaths.PRODUCT_LICENSE_RESOURCE_PATH)
public class DefaultApiProductLicenseResource
    implements ApiProductLicenseResource
{
  private final ProductLicenseService productLicenseService;

  @Inject
  public DefaultApiProductLicenseResource(ProductLicenseService productLicenseService) {
    this.productLicenseService = productLicenseService;
  }

  @Override
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_PLAIN)
  @UnlicensedPath
  @Audited(AuditEvent.INSTALL_LICENSE)
  public Response installLicense(
      @FormDataParam("file") InputStream inputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail)
  {
    productLicenseService.installLicense(inputStream, fileDetail.getFileName());
    return Response.ok().build();
  }

  @Override
  @DELETE
  @Audited(AuditEvent.UNINSTALL_LICENSE)
  public void uninstallLicense() {
    productLicenseService.uninstallLicense();
  }
}
