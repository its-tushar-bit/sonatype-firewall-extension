/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.util.Map;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.service.TenantConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

@Named
@MtiqAdminEndpoint
@Path(AdminApiPaths.ADMIN_CONFIG_PATH)
public class TenantConfigurationResource
{
  private final TenantConfigurationService tenantConfigurationService;

  @Inject
  public TenantConfigurationResource(final TenantConfigurationService tenantConfigurationService) {
    this.tenantConfigurationService = tenantConfigurationService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Object> getConfiguration(
      @PathParam("tenantSlug") String tenantSlug,
      @QueryParam("property") Set<String> properties)
  {
    return tenantConfigurationService.getPropertiesConfiguration(tenantSlug, properties);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_TENANT_CONFIGURATION)
  public void setPropertiesConfiguration(
      @PathParam("tenantSlug") String tenantSlug,
      Map<String, Object> propertiesConfiguration)
  {
    tenantConfigurationService.setPropertiesConfiguration(tenantSlug, propertiesConfiguration);
  }

  @DELETE
  @Audited(AuditEvent.DELETE_TENANT_CONFIGURATION)
  public void deleteConfiguration(
      @PathParam("tenantSlug") String tenantSlug,
      @QueryParam("property") Set<String> properties)
  {
    tenantConfigurationService.deletePropertiesConfiguration(tenantSlug, properties);
  }
}
