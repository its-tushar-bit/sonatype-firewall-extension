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
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;

@Named
@MtiqAdminEndpoint
@Path(AdminApiPaths.ADMIN_SUPPORT_INFO_PATH)
public class SupportInfoResource
{
  private final SupportInfoService supportInfoService;

  @Inject
  public SupportInfoResource(SupportInfoService supportInfoService) {
    this.supportInfoService = supportInfoService;
  }

  @GET
  @Produces("application/zip")
  public Response getSupportZip() throws IOException {
    final File supportZip = supportInfoService.getSupportZip();

    final ResponseBuilder response = Response.ok();
    response.entity(supportZip);
    response.header(HttpHeaders.CONTENT_DISPOSITION,
        HttpHeaderUtils.buildContentDispositionHeaderValue(supportZip.getName()));

    return response.build();
  }
}
