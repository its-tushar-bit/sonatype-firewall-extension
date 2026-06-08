/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.resource;

import java.io.IOException;
import java.util.List;

import com.sonatype.guide.api.controller.GuideVulnerabilitiesApi;
import com.sonatype.guide.api.dto.AffectedComponentVersion;
import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.guide.api.dto.VulnerabilityDetailDocument;
import com.sonatype.guide.api.dto.VulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersionRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchRequest;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Named
@Singleton
@Path("/api/v2/guide/vulnerabilities")
@Produces(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(LicensedFeature.GUIDE_SEARCH)
public class GuideVulnerabilitiesResource
    implements GuideVulnerabilitiesApi
{

  private final SearchApiClient searchApiClient;

  @Inject
  public GuideVulnerabilitiesResource(SearchApiClient searchApiClient) {
    this.searchApiClient = searchApiClient;
  }

  @GET
  @Path("/search")
  @Override
  @SuppressWarnings("unused")
  public ApiSearchResponse<VulnerabilityDocument> searchVulnerabilities(
      @QueryParam("query") String query,
      @QueryParam("offset") Integer offset,
      @QueryParam("limit") Integer limit,
      @QueryParam("sortField") String sortField,
      @QueryParam("sortOrder") String sortOrder,
      @QueryParam("severities") List<String> severities,
      @QueryParam("minCvss") Double minCvss,
      @QueryParam("maxCvss") Double maxCvss,
      @QueryParam("minEpss") Double minEpss,
      @QueryParam("maxEpss") Double maxEpss,
      @QueryParam("hasMalware") Boolean hasMalware,
      @QueryParam("patchAvailable") Boolean patchAvailable,
      @QueryParam("policyCompliant") Boolean policyCompliant,
      @QueryParam("cwes") List<String> cwes,
      @QueryParam("exploitationKnown") Boolean exploitationKnown,
      @QueryParam("publishedWindow") String publishedWindow,
      @QueryParam("affectedEcosystems") List<String> affectedEcosystems,
      @QueryParam("minDocCount") Integer minDocCount) throws IOException
  {
    // policyCompliant is not supported - policy evaluation is a Lifecycle-specific feature.
    // The parameter is accepted for API contract compatibility but ignored.
    GuideVulnerabilitySearchRequest request = new GuideVulnerabilitySearchRequest(
        query, offset, limit, sortField, sortOrder, severities, minCvss, maxCvss,
        minEpss, maxEpss, hasMalware, patchAvailable, cwes, exploitationKnown,
        publishedWindow, affectedEcosystems, minDocCount);
    return searchApiClient.searchVulnerabilities(request);
  }

  @GET
  @Path("/{id}")
  @Override
  public VulnerabilityDetailDocument getVulnerabilityByRefId(@PathParam("id") String id) throws IOException {
    requireNonBlankId(id);
    return searchApiClient.getVulnerabilityByRefId(id);
  }

  @GET
  @Path("/{id}/components")
  @Override
  public ApiSearchResponse<AffectedComponentVersion> getVulnerabilityAffectedComponents(
      @PathParam("id") String id,
      @QueryParam("query") String query,
      @QueryParam("offset") Integer offset,
      @QueryParam("limit") Integer limit,
      @QueryParam("sortField") String sortField,
      @QueryParam("sortOrder") String sortOrder) throws IOException
  {
    requireNonBlankId(id);
    GuideAffectedComponentVersionRequest request = new GuideAffectedComponentVersionRequest(
        id, query, offset, limit, sortField, sortOrder);
    return searchApiClient.getVulnerabilityAffectedComponents(request);
  }

  private static void requireNonBlankId(String id) {
    if (id == null || id.isBlank()) {
      throw new GuideApiException(Response.Status.BAD_REQUEST, "id is required");
    }
  }
}
