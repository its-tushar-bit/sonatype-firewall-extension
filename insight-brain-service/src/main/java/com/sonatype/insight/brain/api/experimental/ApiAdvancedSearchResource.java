/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

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

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.query.SearchService;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchSuggestionResultDTO;

import com.codahale.metrics.annotation.Timed;

/**
 * @since GLOBAL_SEARCH
 */
@Named
@Timed
@Path(PublicApiPaths.ADVANCED_SEARCH_RESOURCE_PATH)
public class ApiAdvancedSearchResource
{
  private final SearchService searchService;

  private final IndexService indexService;

  static final String INDEX_PATH = "index";

  static final String SUGGESTER_PATH = "suggester";

  @Inject
  public ApiAdvancedSearchResource(SearchService searchService, IndexService indexService) {
    this.searchService = searchService;
    this.indexService = indexService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.PERFORM_ADVANCED_SEARCH)
  public SearchResultDTO searchIndex(
      @QueryParam("search") String searchQuery,
      @DefaultValue("10") @QueryParam("pageSize") int pageSize,
      @DefaultValue("1") @QueryParam("page") int page) throws IOException
  {
    return searchService.searchIndex(searchQuery, pageSize, page);
  }

  @POST
  @Path(INDEX_PATH)
  public void createSearchIndexAsync() {
    indexService.createSearchIndexAsync();
  }

  @GET
  @Path(SUGGESTER_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public SearchSuggestionResultDTO autoCompleteSearchQuery(@QueryParam("search") String searchQuery) {
    return searchService.autoCompleteSearchQuery(searchQuery);
  }
}
