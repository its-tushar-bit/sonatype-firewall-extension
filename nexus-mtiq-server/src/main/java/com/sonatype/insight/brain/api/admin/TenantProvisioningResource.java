/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.service.TenantProvisioningService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

@Named
@MtiqAdminEndpoint
@Path(AdminApiPaths.ADMIN_TENANT_PROVISIONING_PATH)
public class TenantProvisioningResource
{
  private final TenantProvisioningService tenantProvisioningService;

  @Inject
  public TenantProvisioningResource(TenantProvisioningService tenantProvisioningService) {
    this.tenantProvisioningService = tenantProvisioningService;
  }

  @POST
  @Audited(AuditEvent.PROVISION_TENANT)
  public void provisionTenant(@PathParam("tenantSlug") String tenantSlug) {
    tenantProvisioningService.provisionTenant(tenantSlug);
  }
}
