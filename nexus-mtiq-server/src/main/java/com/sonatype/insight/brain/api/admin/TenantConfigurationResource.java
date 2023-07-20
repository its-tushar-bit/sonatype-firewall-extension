/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
      @QueryParam("property") Set<String> properties
  )
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
      @QueryParam("property") Set<String> properties
  )
  {
    tenantConfigurationService.deletePropertiesConfiguration(tenantSlug, properties);
  }
}
