/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;

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
  public static final String UPDATE_SAML_CONFIGURATION_PATH = "/saml";

  public static final String GRANT_ADMIN_PERMISSIONS_PATH = "/users/admin";

  private final TenantSecurityConfigurationService tenantSecurityConfigurationService;

  @Inject
  public TenantSecurityConfigurationResource(TenantSecurityConfigurationService tenantSecurityConfigurationService) {
    this.tenantSecurityConfigurationService = tenantSecurityConfigurationService;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_TENANT_SECURITY)
  public void updateSamlConfigurationAndGrantAdminPermissions(
      SecurityConfigurationDTO securityConfiguration,
      @PathParam("tenantSlug") String tenantSlug)
  {
    tenantSecurityConfigurationService.updateSamlConfigurationAndGrantAdminPermissions(securityConfiguration,
        tenantSlug);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_TENANT_SECURITY)
  @Path(UPDATE_SAML_CONFIGURATION_PATH)
  public void updateSamlConfiguration(
      SecurityConfigurationDTO securityConfiguration,
      @PathParam("tenantSlug") String tenantSlug)
  {
    tenantSecurityConfigurationService.updateSamlConfiguration(securityConfiguration, tenantSlug);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_TENANT_SECURITY)
  @Path(GRANT_ADMIN_PERMISSIONS_PATH)
  public void grantAdminPermissionForAdmins(
      List<String> adminEmails,
      @PathParam("tenantSlug") String tenantSlug)
  {
    tenantSecurityConfigurationService.grantAdminPermissionForAdmins(adminEmails, tenantSlug);
  }
}
