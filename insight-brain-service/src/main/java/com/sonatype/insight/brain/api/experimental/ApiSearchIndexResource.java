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
@Path(PublicApiPaths.SEARCH_INDEX_RESOURCE_PATH)
public class ApiSearchIndexResource
{
  private final SearchService searchService;

  private final IndexService indexService;

  static final String SUGGESTER = "suggester";

  @Inject
  public ApiSearchIndexResource(SearchService searchService, IndexService indexService) {
    this.searchService = searchService;
    this.indexService = indexService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public SearchResultDTO searchIndex(
      @QueryParam("search") String searchQuery,
      @DefaultValue("10") @QueryParam("pageSize") int pageSize,
      @DefaultValue("1") @QueryParam("page") int page) throws Exception
  {
    return searchService.searchIndex(searchQuery, pageSize, page);
  }

  @POST
  public void createSearchIndex() throws IOException {
    indexService.createSearchIndex();
  }

  @GET
  @Path(SUGGESTER)
  @Produces(MediaType.APPLICATION_JSON)
  public SearchSuggestionResultDTO autoCompleteSearchQuery(@QueryParam("search") String searchQuery) throws Exception {
    return searchService.autoCompleteSearchQuery(searchQuery);
  }
}
