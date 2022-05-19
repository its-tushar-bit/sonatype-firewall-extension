/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.query.SearchService;
import com.sonatype.insight.brain.search.results.SearchResultDTO;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.88
 */
@Named
@Timed
@Path(PublicApiPaths.ADVANCED_SEARCH_RESOURCE_PATH_V2)
public class DefaultApiAdvancedSearchResourceV2
    implements ApiAdvancedSearchResourceV2
{
  private final SearchService searchService;

  private final IndexService indexService;

  static final String INDEX_PATH = "index";

  static final String EXPORT_CSV_REPORT_PATH = "export/csv";

  @Inject
  public DefaultApiAdvancedSearchResourceV2(SearchService searchService, IndexService indexService) {
    this.searchService = searchService;
    this.indexService = indexService;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.PERFORM_ADVANCED_SEARCH)
  public SearchResultDTO searchIndex(
      @QueryParam("query") String searchQuery,
      @DefaultValue("10") @QueryParam("pageSize") int pageSize,
      @QueryParam("page") int page,
      @DefaultValue("false") @QueryParam("allComponents") boolean allComponents) throws IOException
  {
    return searchService.searchIndex(searchQuery, pageSize, page, allComponents);
  }

  @Override
  @POST
  @Path(INDEX_PATH)
  public void createSearchIndexAsync() {
    indexService.createSearchIndexAsync();
  }

  @GET
  @Path(EXPORT_CSV_REPORT_PATH)
  @Produces("application/csv")
  public Response getExportResults(
      @QueryParam("query") String searchQuery,
      @DefaultValue("false") @QueryParam("allComponents") boolean allComponents)
  {
    return searchService.exportSearch(searchQuery, allComponents);
  }
}
