/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.service.TenantSchemaService;

@Named
@MtiqAdminEndpoint
@Path(AdminApiPaths.ADMIN_TENANT_SCHEMA_PATH)
public class TenantSchemaResource
{
  private final TenantSchemaService tenantSchemaService;

  @Inject
  public TenantSchemaResource(TenantSchemaService tenantSchemaService) {
    this.tenantSchemaService = tenantSchemaService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Integer> getSchemaVersions(@PathParam("tenantSlug") String tenantSlug) {
    return tenantSchemaService.getSchemaVersions(tenantSlug);
  }
}
