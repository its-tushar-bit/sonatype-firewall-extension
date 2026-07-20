/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import java.util.Set;

import com.codahale.metrics.annotation.Timed;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.search.global.SearchSource;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.PermissionService;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Named
@Singleton
@Timed
@Path(CatalogResource.RESOURCE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CatalogResource
{
  public static final String RESOURCE_PATH = "rest/search/catalog";

  private final CatalogService catalogService;

  private final PermissionService permissionService;

  private final CurrentUser currentUser;

  @Inject
  public CatalogResource(
      final CatalogService catalogService,
      final PermissionService permissionService,
      final CurrentUser currentUser)
  {
    this.catalogService = catalogService;
    this.permissionService = permissionService;
    this.currentUser = currentUser;
  }

  /**
   * Status matrix for the three "unavailable" conditions:
   * <ul>
   * <li>{@code GLOBAL_SEARCH} feature off &rarr; 404: the whole endpoint is hidden, so an
   * unreachable route is reported rather than an empty result.</li>
   * <li>{@code CATALOG_FEDERATION} feature off &rarr; 200 with a degraded body
   * ({@code catalogAvailable=false}): the endpoint is reachable but the catalog source is turned
   * off, so the caller gets a well-formed "no catalog" response instead of an error.</li>
   * <li>SBOM-Manager-only local tenant &rarr; 404: this endpoint is Lifecycle-only, so a tenant
   * whose license fails the mode check is treated as not-found, matching the flag-off route.</li>
   * </ul>
   */
  @POST
  public CatalogResponse search(final CatalogRequest request) {
    verifyGlobalSearchEnabled();
    if (request == null) {
      throw new BadRequestException("request body must not be empty");
    }
    verifyReadOnAnyContext();
    final CatalogEntityType entityType = parseEntityType(request.getEntityType());
    final SearchSource source = parseSource(request.getSource());
    return catalogService.search(entityType, source, request);
  }

  private static CatalogEntityType parseEntityType(final String raw) {
    try {
      return CatalogEntityType.fromWireValue(raw);
    }
    catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  private static SearchSource parseSource(final String raw) {
    try {
      return SearchSource.fromWireValue(raw);
    }
    catch (IllegalArgumentException e) {
      throw new BadRequestException("unknown source");
    }
  }

  private static void verifyGlobalSearchEnabled() {
    if (!SystemConfigurationPropertyFeature.GLOBAL_SEARCH.isEnabled()) {
      throw new NotFoundException("Not Found");
    }
  }

  private void verifyReadOnAnyContext() {
    // Anonymous callers are rejected upstream with 401 by the Shiro requireAuth filter, so any request
    // reaching here is authenticated; a caller with no readable context is authenticated-but-forbidden.
    final Set<String> readContextIds =
        permissionService.getContextIdsForUserWithPermission(currentUser.getUserPrincipal(), Permission.READ);
    if (readContextIds.isEmpty()) {
      throw new ForbiddenException("Not authorized");
    }
  }
}
