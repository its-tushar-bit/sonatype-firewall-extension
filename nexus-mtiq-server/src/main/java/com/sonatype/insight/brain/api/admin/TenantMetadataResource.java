/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.dto.TenantMetadataDTO;
import com.sonatype.insight.brain.api.admin.service.TenantMetadataConfigurationService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;

@Named
@MtiqAdminEndpoint
@Path(AdminApiPaths.ADMIN_TENANT_METADATA_PATH)
public class TenantMetadataResource
{
  private final TenantMetadataConfigurationService tenantMetadataConfigurationService;

  @Inject
  public TenantMetadataResource(TenantMetadataConfigurationService tenantMetadataConfigurationService) {
    this.tenantMetadataConfigurationService = tenantMetadataConfigurationService;
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_TENANT_METADATA)
  public void updateMetadata(
      TenantMetadataDTO tenantMetadataDTO,
      @PathParam("tenantSlug") String tenantSlug)
  {
    tenantMetadataConfigurationService.insertOrUpdateMetadata(tenantMetadataDTO, tenantSlug);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public TenantMetadataDTO getMetadata(@PathParam("tenantSlug") String tenantSlug) {
    return tenantMetadataConfigurationService.getMetadata(tenantSlug);
  }
}
