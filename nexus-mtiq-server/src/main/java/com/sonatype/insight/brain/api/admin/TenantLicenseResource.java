/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.io.InputStream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.service.TenantLicenseService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Named
@MtiqAdminEndpoint
@Path(AdminApiPaths.ADMIN_TENANT_LICENSE_PATH)
public class TenantLicenseResource
{
  private final TenantLicenseService tenantLicenseService;

  @Inject
  public TenantLicenseResource(TenantLicenseService tenantLicenseService) {
    this.tenantLicenseService = tenantLicenseService;
  }

  @PUT
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_PLAIN)
  @Audited(AuditEvent.INSTALL_TENANT_LICENSE)
  public void updateLicense(
      @FormDataParam("file") InputStream inputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @PathParam("tenantSlug") String tenantSlug)
  {
    tenantLicenseService.updateLicense(inputStream, fileDetail.getFileName(), tenantSlug);
  }
}
