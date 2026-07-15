/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.resource;

import java.io.IOException;

import com.sonatype.guide.api.controller.GuideRecommendationsApi;
import com.sonatype.guide.api.dto.RecommendationResponse;
import com.sonatype.guide.api.request.RecommendationRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideRecommendationResult;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.api.error.GuidePurlValidator;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.guide.policy.GuidePolicyService;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Named
@Singleton
@Path("/api/v2/guide/recommendations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(LicensedFeature.GUIDE_SEARCH)
public class GuideRecommendationsResource
    implements GuideRecommendationsApi
{
  private final SearchApiClient searchApiClient;

  private final GuidePolicyService guidePolicyService;

  @Inject
  public GuideRecommendationsResource(
      SearchApiClient searchApiClient,
      GuidePolicyService guidePolicyService)
  {
    this.searchApiClient = searchApiClient;
    this.guidePolicyService = guidePolicyService;
  }

  @Override
  public RecommendationResponse getRecommendations(RecommendationRequest request) throws IOException {
    return getRecommendations(request, null, null);
  }

  @POST
  @Override
  public RecommendationResponse getRecommendations(
      RecommendationRequest request,
      @Parameter(description = "Restrict policy evaluation to this owner (application or organization "
          + "id). Omitted defaults to the root organization.") @QueryParam("ownerId") String ownerId,
      @Parameter(description = "Policy evaluation stage: develop, build, stage-release, release, or "
          + "operate. Case-insensitive; omitted defaults to release.") @QueryParam("stage") String stage) throws IOException
  {
    // request itself is null when JAX-RS receives an empty or JSON-`null` body. Without this
    // guard the next line NPEs and Dropwizard's default handler returns a non-Guide envelope.
    if (request == null || request.purl() == null || request.purl().isBlank()) {
      throw new GuideApiException(Response.Status.BAD_REQUEST, "Purl is required");
    }
    GuidePolicyService.requireValidStage(stage);
    GuidePurlValidator.validate(request.purl());
    GuideRecommendationResult upstream = searchApiClient.getRecommendations(request.purl());
    return guidePolicyService.filterRecommendations(upstream, request.purl(), ownerId, stage);
  }
}
