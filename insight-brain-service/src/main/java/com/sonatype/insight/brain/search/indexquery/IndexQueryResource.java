/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import java.util.Set;

import com.codahale.metrics.annotation.Timed;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.search.index.SearchIndexClient;

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

/**
 * JAX-RS resource backing the IQ-index left-nav list pages at {@code POST /rest/search/index-query}.
 *
 * <p>
 * Gated by the {@code PREVIEW_NEXUS_ONE_UI} feature flag, the same flag that gates the Nexus One UI
 * pages this endpoint backs. When disabled callers receive {@code 404 Not Found}.
 */
@Named
@Singleton
@Timed
@Path(IndexQueryResource.RESOURCE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IndexQueryResource
{
  public static final String RESOURCE_PATH = "rest/search/index-query";

  private final IndexQueryService indexQueryService;

  private final SearchIndexClient searchIndexClient;

  @Inject
  public IndexQueryResource(
      final IndexQueryService indexQueryService,
      final SearchIndexClient searchIndexClient)
  {
    this.indexQueryService = indexQueryService;
    this.searchIndexClient = searchIndexClient;
  }

  @POST
  public IndexQueryResponse query(final IndexQueryRequest request) {
    verifyPreviewUiEnabled();
    if (request == null) {
      throw new BadRequestException("request body must not be empty");
    }
    verifyReadOnAnyContext();
    final IndexQueryType queryType = parseQueryType(request.getEntityType());
    return indexQueryService.query(queryType, request);
  }

  private static IndexQueryType parseQueryType(final String raw) {
    try {
      return IndexQueryType.fromWireValue(raw);
    }
    catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  /**
   * Gate on {@code PREVIEW_NEXUS_ONE_UI}, the flag that gates the Nexus One UI this endpoint backs.
   *
   * <p>
   * 404 (not 403) when the flag is off, so a disabled endpoint is indistinguishable from absent. Called
   * as the first statement of the handler so the gate runs ahead of body validation.
   */
  private static void verifyPreviewUiEnabled() {
    if (!SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.isEnabled()) {
      throw new NotFoundException("Not Found");
    }
  }

  private void verifyReadOnAnyContext() {
    // Anonymous callers are rejected upstream with 401 by the Shiro requireAuth filter, so any request
    // reaching here is authenticated; a caller with no readable context is authenticated-but-forbidden.
    // Gate on the same read-context source the row filter uses, so the gate and filter cannot diverge.
    final Set<String> readContextIds = searchIndexClient.getCurrentUserContextIdsWithReadPermission();
    if (readContextIds.isEmpty()) {
      throw new ForbiddenException("Not authorized");
    }
  }
}
