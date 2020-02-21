/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.io.IOException;
import java.util.Locale;

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
import com.sonatype.insight.brain.vulnerability.VulnerabilityDetailResource;

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

  private final VulnerabilityDetailResource vulnerabilityDetailResource;

  private static final String SUGGESTER = "suggester";

  @Inject
  public ApiSearchIndexResource(
      SearchService searchService,
      IndexService indexService,
      VulnerabilityDetailResource vulnerabilityDetailResource)
  {
    this.searchService = searchService;
    this.indexService = indexService;
    this.vulnerabilityDetailResource = vulnerabilityDetailResource;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public SearchResultDTO searchIndex(
      @QueryParam("search") String search,
      @DefaultValue("10") @QueryParam("pageSize") int pageSize,
      @DefaultValue("1") @QueryParam("page") int page)
      throws Exception
  {
    return searchService.searchIndex(search, pageSize, page);
  }

  @POST
  public void createSearchIndex() throws IOException {
    indexService.createSearchIndex(this::getHtml);
  }

  private String getHtml(String refId) {
    String refIdLower = refId.toLowerCase(Locale.ROOT);
    String source;
    if (refIdLower.startsWith("cve")) {
      source = "cve";
    }
    else if (refIdLower.startsWith("sonatype")) {
      source = "sonatype";
    }
    else {
      return refIdLower;
    }
    return vulnerabilityDetailResource.getDetails(source, refId, null, null, null, null, null, null).getHtmlDetails();
  }

  @GET
  @Path(SUGGESTER)
  @Produces(MediaType.APPLICATION_JSON)
  public SearchSuggestionResultDTO autoCompleteSearchQuery(@QueryParam("search") String searchQuery) throws Exception {
    return searchService.autoCompleteSearchQuery(searchQuery);
  }
}
