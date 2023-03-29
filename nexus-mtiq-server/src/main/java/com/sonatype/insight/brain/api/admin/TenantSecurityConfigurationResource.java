/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.dto.SecurityConfigurationDTO;
import com.sonatype.insight.brain.api.admin.service.TenantSecurityConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

@Named
@MtiqAdminEndpoint
@Path(AdminApiPaths.ADMIN_TENANT_SECURITY_CONFIG_PATH)
public class TenantSecurityConfigurationResource
{
  private final TenantSecurityConfigurationService tenantSecurityConfigurationService;

  @Inject
  public TenantSecurityConfigurationResource(TenantSecurityConfigurationService tenantSecurityConfigurationService) {
    this.tenantSecurityConfigurationService = tenantSecurityConfigurationService;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_TENANT_SECURITY)
  public void updateSecurityConfiguration(
      SecurityConfigurationDTO securityConfiguration,
      @PathParam("tenantSlug") String tenantSlug)
  {
    tenantSecurityConfigurationService.updateSamlConfigurationAndGrantAdminPermissions(securityConfiguration,
        tenantSlug);
  }
}
