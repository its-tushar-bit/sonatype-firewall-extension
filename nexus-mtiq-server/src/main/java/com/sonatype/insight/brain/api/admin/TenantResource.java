/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.service.TenantService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

@Named
@MtiqAdminEndpoint
@Path(AdminApiPaths.ADMIN_PATH)
public class TenantResource
{
  public static final String LIST_TENANTS = "list-tenants";

  private final TenantService tenantService;

  @Inject
  public TenantResource(TenantService tenantService) {
    this.tenantService = tenantService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.LIST_TENANTS)
  @Path(LIST_TENANTS)
  public List<String> getAllTenantsNames() {
    return tenantService.getAllTenantsNames();
  }
}
