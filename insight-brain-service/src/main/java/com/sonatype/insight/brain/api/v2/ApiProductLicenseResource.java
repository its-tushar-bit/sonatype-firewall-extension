/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.InputStream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseService;
import com.sonatype.insight.brain.product.license.UnlicensedPath;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 * @since 1.114
 */
@Named
@Timed
@Path(PublicApiPaths.PRODUCT_LICENSE_RESOURCE_PATH)
@Tag(name = "Product License",
    description = "Use this REST API to manage a product license.")
public class ApiProductLicenseResource
{
  private final ProductLicenseService productLicenseService;

  @Inject
  public ApiProductLicenseResource(ProductLicenseService productLicenseService) {
    this.productLicenseService = productLicenseService;
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @UnlicensedPath
  @Audited(AuditEvent.INSTALL_LICENSE)
  @Operation(description = "Use this method to install a product license" +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "License installed successfully")
      })
  public Response installLicense(
      @Parameter(required = true,
          schema = @Schema(type = "string", format = "binary", description = "Your product license file"))
      @FormDataParam("file") InputStream inputStream,
      @Parameter(hidden = true)
      @FormDataParam("file") FormDataContentDisposition fileDetail)
  {
    productLicenseService.installLicense(inputStream, fileDetail.getFileName());
    return Response.ok().build();
  }

  @DELETE
  @Audited(AuditEvent.UNINSTALL_LICENSE)
  @Operation(description = "Use this method to uninstall a product license." +
      "\n" +
      "\n" +
      "Permissions required: Edit System Configuration and Users",
      responses = {
          @ApiResponse(responseCode = "204",
              description = "License uninstalled successfully")
      })
  public void uninstallLicense() {
    productLicenseService.uninstallLicense();
  }
}
