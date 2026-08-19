/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.resource;

import static com.sonatype.insight.brain.guide.policy.GuidePolicyService.requireLimitWithinPolicyEnrichmentCap;
import static com.sonatype.insight.brain.guide.policy.GuidePolicyService.requireValidStage;

import java.io.IOException;
import java.util.List;

import com.sonatype.guide.api.controller.GuideVulnerabilitiesApi;
import com.sonatype.guide.api.dto.AffectedComponentVersion;
import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.guide.api.dto.VulnerabilityDetailDocument;
import com.sonatype.guide.api.dto.VulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersionRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchRequest;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.guide.policy.GuidePolicyService;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Named
@Singleton
@Path("/api/v2/guide/vulnerabilities")
@Produces(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(value = LicensedFeature.GUIDE_SEARCH, anyOf = LicensedFeature.AI_DEVELOPER)
public class GuideVulnerabilitiesResource
    implements GuideVulnerabilitiesApi
{

  private final SearchApiClient searchApiClient;

  private final GuidePolicyService guidePolicyService;

  @Inject
  public GuideVulnerabilitiesResource(
      SearchApiClient searchApiClient,
      GuidePolicyService guidePolicyService)
  {
    this.searchApiClient = searchApiClient;
    this.guidePolicyService = guidePolicyService;
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
    GuideValidation.requireNonBlankId(id, "id");
    return searchApiClient.getVulnerabilityByRefId(id);
  }

  @Override
  public ApiSearchResponse<AffectedComponentVersion> getVulnerabilityAffectedComponents(
      String id,
      String query,
      Integer offset,
      Integer limit,
      String sortField,
      String sortOrder) throws IOException
  {
    return getVulnerabilityAffectedComponents(id, query, offset, limit, sortField, sortOrder, null, null);
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
      @QueryParam("sortOrder") String sortOrder,
      @Parameter(description = "Restrict policy evaluation to this owner (application or organization "
          + "id). Omitted defaults to the root organization.") @QueryParam("ownerId") String ownerId,
      @Parameter(description = "Policy evaluation stage: develop, build, stage-release, release, or "
          + "operate. Case-insensitive; omitted defaults to release.") @QueryParam("stage") String stage) throws IOException
  {
    GuideValidation.requireNonBlankId(id, "id");
    requireLimitWithinPolicyEnrichmentCap(limit);
    requireValidStage(stage);
    GuideAffectedComponentVersionRequest request = new GuideAffectedComponentVersionRequest(
        id, query, offset, limit, sortField, sortOrder);
    return guidePolicyService.enrichAffectedSearch(
        searchApiClient.getVulnerabilityAffectedComponents(request), ownerId, stage);
  }
}
