/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.resource;

import java.io.IOException;
import java.util.List;

import com.sonatype.guide.api.controller.GuideGlobalSearchApi;
import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.guide.api.dto.SearchResult;
import com.sonatype.insight.brain.guide.api.dto.GuideGlobalSearchRequest;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Named
@Singleton
@Path("/api/v2/guide/global")
@Produces(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(LicensedFeature.GUIDE_SEARCH)
public class GuideGlobalSearchResource
    implements GuideGlobalSearchApi
{

  private final SearchApiClient searchApiClient;

  @Inject
  public GuideGlobalSearchResource(SearchApiClient searchApiClient) {
    this.searchApiClient = searchApiClient;
  }

  @GET
  @Path("/search")
  @Override
  public ApiSearchResponse<SearchResult> globalSearch(
      @QueryParam("query") String query,
      @QueryParam("offset") Integer offset,
      @QueryParam("limit") Integer limit,
      @QueryParam("sortField") String sortField,
      @QueryParam("sortOrder") String sortOrder,
      @QueryParam("latestStable") String latestStable,
      @QueryParam("formats") List<String> formats,
      @QueryParam("publishedWindow") String publishedWindow) throws IOException
  {
    GuideGlobalSearchRequest request = new GuideGlobalSearchRequest(
        query, offset, limit, sortField, sortOrder, latestStable, formats, publishedWindow);
    return searchApiClient.globalSearch(request);
  }
}
