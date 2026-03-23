/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.dto.TenantCacheStatisticsDTO;
import com.sonatype.insight.brain.api.admin.service.TenantCacheService;

/**
 * Resource for accessing per-tenant cache statistics.
 *
 * @since 1.185
 */
@Named
@MtiqAdminEndpoint
@Path(AdminApiPaths.ADMIN_TENANT_CACHE_PATH)
public class TenantCacheResource
{
  private final TenantCacheService tenantCacheService;

  @Inject
  public TenantCacheResource(TenantCacheService tenantCacheService) {
    this.tenantCacheService = tenantCacheService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public TenantCacheStatisticsDTO getCacheStatistics(@PathParam("tenantSlug") String tenantSlug) {
    return tenantCacheService.getCacheStatistics(tenantSlug);
  }
}
