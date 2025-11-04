/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.v2.dto.SsoConfigurationDTO;
import com.sonatype.insight.brain.api.admin.service.TenantSsoConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

@Named
@MtiqAdminEndpoint
@Path(AdminApiPaths.ADMIN_TENANT_SSO_CONFIGURATION_PATH)
public class TenantSsoConfigurationResource
{
  public static final String SYNC_PATH = "/sync";

  private final TenantSsoConfigurationService tenantSsoConfigurationService;

  @Inject
  public TenantSsoConfigurationResource(TenantSsoConfigurationService tenantSsoConfigurationService) {
    this.tenantSsoConfigurationService = tenantSsoConfigurationService;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_TENANT_SS0_CONFIGURATION)
  public void updateSsoConfiguration(
      SsoConfigurationDTO ssoConfigurationDTO,
      @PathParam("tenantSlug") String tenantSlug)
  {
    tenantSsoConfigurationService.updateSsoConfiguration(ssoConfigurationDTO, tenantSlug);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path(SYNC_PATH)
  public void syncSsoProviderDataSources(@PathParam("tenantSlug") String tenantSlug) {
    tenantSsoConfigurationService.syncSsoProviderDataSources(tenantSlug);
  }
}
