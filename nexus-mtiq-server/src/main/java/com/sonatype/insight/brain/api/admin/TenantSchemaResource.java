/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.service.TenantSchemaService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

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

  @PUT
  @Audited(AuditEvent.MIGRATE_TENANT)
  public void migrateSchema(@PathParam("tenantSlug") String tenantSlug) {
    tenantSchemaService.migrateSchema(tenantSlug);
  }
}
