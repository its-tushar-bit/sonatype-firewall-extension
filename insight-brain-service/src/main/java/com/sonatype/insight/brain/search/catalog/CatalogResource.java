/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import java.util.Iterator;
import java.util.Set;

import com.codahale.metrics.annotation.Timed;
import com.sonatype.insight.brain.api.CsvMediaType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.search.export.CatalogCsvColumns;
import com.sonatype.insight.brain.search.export.CsvStreamingResponse;
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
import jakarta.ws.rs.core.Response;

@Named
@Singleton
@Timed
@Path(CatalogResource.RESOURCE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CatalogResource
{
  public static final String RESOURCE_PATH = "rest/search/catalog";

  /** Sub-path of {@link #RESOURCE_PATH} serving the streaming CSV export of the My-Scan-Data list. */
  public static final String EXPORT_CSV_PATH = "export/csv";

  /** 400 message when a CSV export is requested for the catalog (Guide/HDS) source. */
  static final String CATALOG_SOURCE_NOT_EXPORTABLE =
      "CSV export is available for the My Scan Data source only";

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
   * Status matrix for the "unavailable" conditions:
   * <ul>
   * <li>{@code PREVIEW_NEXUS_ONE_UI} feature off &rarr; 404: the whole endpoint is hidden, so an
   * unreachable route is reported rather than an empty result.</li>
   * <li>catalog (Guide/HDS) backend failure &rarr; 200 with a degraded body
   * ({@code catalogAvailable=false}): the endpoint is reachable but the upstream catalog source is
   * unavailable, so the caller gets a well-formed "no catalog" response instead of an error.</li>
   * <li>SBOM-Manager-only local tenant &rarr; 404: this endpoint is Lifecycle-only, so a tenant
   * whose license fails the mode check is treated as not-found.</li>
   * </ul>
   */
  @POST
  public CatalogResponse search(final CatalogRequest request) {
    verifyPreviewUiEnabled();
    if (request == null) {
      throw new BadRequestException("request body must not be empty");
    }
    verifyReadOnAnyContext();
    final CatalogEntityType entityType = parseEntityType(request.getEntityType());
    final SearchSource source = parseSource(request.getSource());
    return catalogService.search(entityType, source, request);
  }

  /**
   * Streaming CSV of the SAME My-Scan-Data list the {@code POST} above returns: identical filters,
   * sort, RBAC scoping, row mapping and per-page enrichment, minus pagination (the whole filtered
   * result set is written, up to the documented row cap).
   *
   * <p>
   * LOCAL source only. A {@code source=catalog} export is rejected with 400 rather than silently
   * exporting local rows under a catalog-looking request: the catalog leg reads a remote Guide/HDS
   * store that is offset-paginated with a hard page ceiling and no cursor, so it cannot be walked to
   * completion from here. Rejecting is the honest answer; falling through to local data would be a
   * wrong answer that looks right.
   *
   * <p>
   * Gates run in the same order as the list endpoint (flag, then body, then RBAC), so an export cannot
   * be reachable in a state where the list is not.
   */
  @POST
  @Path(EXPORT_CSV_PATH)
  @Produces(CsvMediaType.TEXT_CSV)
  public Response exportCsv(final CatalogRequest request) {
    verifyPreviewUiEnabled();
    if (request == null) {
      throw new BadRequestException("request body must not be empty");
    }
    verifyReadOnAnyContext();
    final CatalogEntityType entityType = parseEntityType(request.getEntityType());
    if (parseSource(request.getSource()) == SearchSource.CATALOG) {
      throw new BadRequestException(CATALOG_SOURCE_NOT_EXPORTABLE);
    }
    final Iterator<CatalogRow> rows = catalogService.streamLocalForExport(entityType, request);
    return CsvStreamingResponse.build(
        CatalogCsvColumns.fileNamePrefix(entityType), CatalogCsvColumns.forLocalType(entityType), rows);
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

  /**
   * Gate on {@code PREVIEW_NEXUS_ONE_UI}, the flag that gates the Nexus One UI this endpoint backs.
   * 404 (not 403) when the flag is off, so a disabled endpoint is indistinguishable from absent.
   */
  private static void verifyPreviewUiEnabled() {
    if (!SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.isEnabled()) {
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
