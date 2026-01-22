/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.hds.IntegrationVersionCache;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST API for managing the integration version cache.
 *
 * @since 1.196
 */
@Named
@Timed
@Path(PublicApiPaths.CONFIG_RESOURCE_PATH_V2 + "/integrationVersions/cache")
@Tag(name = "Configuration", description = "Integration version cache management")
public class ApiIntegrationVersionCacheResource
{
  private final IntegrationVersionCache integrationVersionCache;

  @Inject
  public ApiIntegrationVersionCacheResource(final IntegrationVersionCache integrationVersionCache) {
    this.integrationVersionCache = integrationVersionCache;
  }

  @DELETE
  @Audited(AuditEvent.CONFIGURE_PROPERTIES)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      description = """
          Clear the integration version cache. \
          Use this endpoint after a new integration version is released to ensure IQ Server \
          immediately recognizes the new version instead of waiting for cache expiration (10 minutes).

          Permissions required: Edit System Configuration and Users""",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Cache cleared successfully. Returns the number of entries that were invalidated.",
              useReturnTypeSchema = true
          )
      }
  )
  public CacheInvalidationResponse invalidateCache() {
    long entriesInvalidated = integrationVersionCache.invalidateAll();
    return new CacheInvalidationResponse(entriesInvalidated);
  }

  public record CacheInvalidationResponse(long entriesInvalidated) {}
}
