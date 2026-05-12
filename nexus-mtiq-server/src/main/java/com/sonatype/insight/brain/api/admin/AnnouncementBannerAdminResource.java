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
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.configuration.AnnouncementBannerService;
import com.sonatype.insight.brain.model.configuration.AnnouncementBanner;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * Sonatype-only admin endpoint for the announcement banner. The banner is deployment-global; operators must use
 * the reserved {@code global} tenant slug (enforced by {@link #requireGlobalTenant}).
 */
@Named
@MtiqAdminEndpoint
@Path(AdminApiPaths.ADMIN_ANNOUNCEMENT_BANNER_PATH)
public class AnnouncementBannerAdminResource
{
  private final AnnouncementBannerService service;

  private final TenantUtil tenantUtil;

  @Inject
  public AnnouncementBannerAdminResource(final AnnouncementBannerService service, final TenantUtil tenantUtil) {
    this.service = service;
    this.tenantUtil = tenantUtil;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public AnnouncementBanner get(@PathParam("tenantSlug") final String tenantSlug) {
    requireGlobalTenant(tenantSlug);
    return service.getBanner();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_ANNOUNCEMENT_BANNER)
  public AnnouncementBanner update(
      @PathParam("tenantSlug") final String tenantSlug,
      final AnnouncementBanner banner)
  {
    requireGlobalTenant(tenantSlug);
    // JAX-RS passes null when the client PUTs with no body; guard before the service/validator so an
    // operator running `curl -X PUT` without --data gets 400, not 500.
    if (banner == null) {
      throw new BadRequestException("Request body is required.");
    }
    return service.updateBanner(banner);
  }

  /**
   * {@code AdminTenantFilter} requires a {tenantSlug} path segment, but the banner is deployment-global; reject
   * any slug other than {@code global} so operator typos can't silently succeed.
   */
  private void requireGlobalTenant(final String tenantSlug) {
    if (!tenantUtil.isGlobalTenant()) {
      throw new BadRequestException(
          "Announcement banner is a deployment-global resource; use tenant slug 'global' (got: '" + tenantSlug
              + "').");
    }
  }
}
