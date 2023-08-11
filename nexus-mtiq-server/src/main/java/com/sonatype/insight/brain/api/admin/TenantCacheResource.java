/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.admin.MtiqAdminEndpoint;
import com.sonatype.insight.brain.api.AdminApiPaths;
import com.sonatype.insight.brain.api.admin.service.TenantCacheService;

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
  public String getCacheStatistics(@PathParam("tenantSlug") String tenantSlug) {
    return tenantCacheService.getCache(tenantSlug);
  }
}
