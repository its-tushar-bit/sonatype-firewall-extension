/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductMode;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.query.SearchService;
import com.sonatype.insight.brain.search.results.SearchResultDTO;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.88
 */
@Named
@Timed
@Path(PublicApiPaths.ADVANCED_SEARCH_RESOURCE_PATH_V2)
@Tag(name = "Advanced Search",
    description = "Use the Advanced Search REST API to perform searches on Lifecycle application scan reports.")
public class ApiAdvancedSearchResourceV2
{
  private final SearchService searchService;

  private final IndexService indexService;

  static final String INDEX_PATH = "index";

  static final String EXPORT_CSV_REPORT_PATH = "export/csv";

  @Inject
  public ApiAdvancedSearchResourceV2(SearchService searchService, IndexService indexService) {
    this.searchService = searchService;
    this.indexService = indexService;
  }

  /**
   * Search request to search the index.
   *
   * @param searchQuery - String holding a query to search for.
   * @param pageSize    - the amount of results per page
   * @param page        - the current page to start from, 0 indexed.
   * @return SearchResultDTO
   * @throws IOException on failing to search the index
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.PERFORM_ADVANCED_SEARCH)
  @Operation(description = "Use this method to perform an Advanced Search. ")
  @ApiResponse(responseCode = "409", description = "Search index does not exist or is unreadable.")
  @ApiResponse(responseCode = "200", description = "Response JSON containing the search query sent in the API call, " +
      "and other response fields as follows: \n" +
      "1. searchQuery: search query sent in the request \n" +
      "2. page: page number of search results requested \n" +
      "3. pageSize: requested number of results per page \n" +
      "4. totalNumberOfHits: total number of results returned \n" +
      "5. isExactTotalNumberOfHits \n" +
      "    * `true` indicates that the search results in the JSON is the same no. of search results that logically  " +
      "    match the search query. \n" +
      "    * `false` indicates that the search results in the JSON are lower bound because fetching all results is " +
      "    too expensive to compute. \n" +
      "6. groupingByDTOS: array of search results grouped on a field name \n" +
      "7. groupIdentifier: field name that the search results have been grouped by \n" +
      "8. groupBy: field value that the search results have been grouped by \n" +
      "9. additionalInfo: shared information between groups, e.g. info if grouped by a security vulnerability \n" +
      "10. searchResultItemDTOS: array of search results with each element containing an itemType, field names " +
      "and values \n" +
      "11. resultIndex: indicating the relevance of the search result w.r.t. the query",
      useReturnTypeSchema = true)
  public SearchResultDTO searchIndex(
      @Parameter(description = "Enter your search query here") @QueryParam("query") String searchQuery,
      @Parameter(description = "Enter the no. of results that should be visible per page") @DefaultValue("10")
      @QueryParam("pageSize") int pageSize,
      @Parameter(description = "Enter the page no. for the page containing results") @QueryParam("page") int page,
      @Parameter(description = "Set to `true` to retrieve results that include components with no violations")
      @DefaultValue("false") @QueryParam("allComponents") boolean allComponents,
      @QueryParam("mode") ProductMode mode,
      @Parameter(hidden = true)
      @QueryParam("searchAfter") String searchAfter)
  {
    return searchService.searchIndex(searchQuery, pageSize, page, allComponents, mode, searchAfter);
  }

  /**
   * Request a Search Index to be created asynchronously.
   */
  @POST
  @Path(INDEX_PATH)
  @Operation(description =
      "Use this method to create or rebuild the index for Advanced Search. " +
          "This is a resource intensive operation. Avoid creating indexes during peak usage hours." +
          "\n" +
          "\n" +
          "Permissions required: Edit System Configuration and Users")
  @ApiResponse(responseCode = "204", description = "Index created successfully.")
  public void createSearchIndexAsync() {
    indexService.createIndexAsync();
  }

  @GET
  @Path(EXPORT_CSV_REPORT_PATH)
  @Produces("application/csv")
  @Operation(description =
      "Use this method to generate a csv file containing your search results. " +
          "The default delimiter in the generated file is comma. " +
          "Use the advancedSearchCSVExportDelimiter property of the Configuration REST API to change the delimiter " +
          "in the generated file."
  )
  @ApiResponse(responseCode = "200", description = "Downloadable csv file generated successfully.")
  @ApiResponse(responseCode = "409", description = "Search index does not exist or is unreadable.")
  public Response getExportResults(
      @Parameter(description = "A well-formed search query.", required = true)
      @QueryParam("query") String searchQuery,
      @Parameter(description = "Enter the no. of results that should be visible per page, unset gives all results")
      @QueryParam("pageSize") Integer pageSize,
      @Parameter(description = "Enter the page no. for the page containing results") @QueryParam("page") int page,
      @Parameter(description = "Set to `true` to retrieve results that include components with no violations.")
      @DefaultValue("false") @QueryParam("allComponents") boolean allComponents,
      @QueryParam("mode") ProductMode mode,
      @Parameter(hidden = true)
      @QueryParam("searchAfter") String searchAfter,
      @Context HttpServletResponse httpServletResponse)
  {
    return searchService.exportSearch(searchQuery, pageSize, page, allComponents, mode, searchAfter,
        httpServletResponse);
  }
}
