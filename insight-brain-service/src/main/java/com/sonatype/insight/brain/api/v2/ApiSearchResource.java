/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.search.query.SearchService;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchSuggestionResultDTO;
import com.sonatype.insight.brain.service.InsightWork;

import com.codahale.metrics.annotation.Timed;

/**
 * @since GLOBAL_SEARCH
 */
@Named
@Timed
@Path(PublicApiPaths.INDEX_RESOURCE_PATH)
public class ApiSearchResource
{
  private final SearchService searchService;

  private final InsightWork insightWork;

  public static final String SUGGESTER = "suggester";

  @Inject
  public ApiSearchResource(SearchService searchService, InsightWork insightWork) {
    this.searchService = searchService;
    this.insightWork = insightWork;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public SearchResultDTO searchApplicationComponentSecurityVulnerabilityIndex(
      @QueryParam("search") String search,
      @DefaultValue("10") @QueryParam("pageSize") int pageSize,
      @DefaultValue("1") @QueryParam("page") int page)
      throws Exception
  {
    return searchService
        .searchApplicationComponentSecurityVulnerabilityIndex(insightWork.getWorkDir().toPath().resolve("index"),
            search, pageSize, page);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(SUGGESTER)
  public SearchSuggestionResultDTO autoCompleteSearchApplicationComponentSecurityVulnerabilityIndex(
      @QueryParam("search") String searchQuery)
      throws Exception
  {
    return searchService
        .autocompleteSearchApplicationComponentSecurityVulnerability(
            insightWork.getWorkDir().toPath().resolve("search-suggester"),
            searchQuery);
  }
}
