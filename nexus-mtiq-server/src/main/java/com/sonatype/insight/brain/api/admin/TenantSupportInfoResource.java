/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.service.TenantSupportInfoService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;

@Named
@MtiqAdminEndpoint
@Path(AdminApiPaths.ADMIN_SUPPORT_INFO_PATH)
public class TenantSupportInfoResource
{
  private final TenantSupportInfoService tenantSupportInfoService;

  @Inject
  public TenantSupportInfoResource(TenantSupportInfoService tenantSupportInfoService) {
    this.tenantSupportInfoService = tenantSupportInfoService;
  }

  @GET
  @Produces("application/zip")
  @Audited(AuditEvent.GENERATE_TENANT_SUPPORT_INFO)
  public Response getSupportZip(@PathParam("tenantSlug") String tenantSlug) throws IOException {
    final File supportZip = tenantSupportInfoService.getSupportZip(tenantSlug);

    final ResponseBuilder response = Response.ok();
    response.entity(supportZip);
    response.header(HttpHeaders.CONTENT_DISPOSITION,
        HttpHeaderUtils.buildContentDispositionHeaderValue(supportZip.getName()));

    return response.build();
  }
}
