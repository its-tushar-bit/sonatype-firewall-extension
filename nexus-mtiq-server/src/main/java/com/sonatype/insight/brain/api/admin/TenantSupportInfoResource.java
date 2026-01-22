/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.StreamingOutput;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.service.TenantSupportInfoService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.support.SupportInfo;
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
  public Response getSupportZip(
      @PathParam("tenantSlug") String tenantSlug) throws IOException
  {
    final SupportInfo supportInfo = tenantSupportInfoService.getSupportInfo(tenantSlug);
    final StreamingOutput streamingOutput = supportInfo.getSupportInfoOutputStream()::writeTo;

    final ResponseBuilder response = Response.ok();
    response.entity(streamingOutput);
    response.header(HttpHeaders.CONTENT_DISPOSITION,
        HttpHeaderUtils.buildContentDispositionHeaderValue(supportInfo.getSupportInfoName()));

    return response.build();
  }
}
