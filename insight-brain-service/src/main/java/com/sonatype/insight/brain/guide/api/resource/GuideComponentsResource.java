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

import com.sonatype.guide.api.controller.GuideComponentsApi;
import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.guide.api.dto.ComponentDetailDocument;
import com.sonatype.guide.api.dto.ComponentDocument;
import com.sonatype.guide.api.dto.VulnerabilityDocument;
import com.sonatype.guide.api.request.LatestVersionRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDependenciesRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentSearchRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentVersionsRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentVulnerabilitiesRequest;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.api.error.GuidePurlValidator;
import com.sonatype.insight.brain.guide.api.purl.GuidePurlAssembler;
import com.sonatype.insight.brain.guide.api.purl.PurlArtifactQualifiers;
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.guide.policy.GuidePolicyService;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Named
@Singleton
@Path("/api/v2/guide/components")
@Produces(MediaType.APPLICATION_JSON)
@ProductLicenseEnforcementPoint(value = LicensedFeature.GUIDE_SEARCH, anyOf = LicensedFeature.AI_DEVELOPER)
public class GuideComponentsResource
    implements GuideComponentsApi
{
  private final SearchApiClient searchApiClient;

  private final GuidePolicyService guidePolicyService;

  @Inject
  public GuideComponentsResource(
      SearchApiClient searchApiClient,
      GuidePolicyService guidePolicyService)
  {
    this.searchApiClient = searchApiClient;
    this.guidePolicyService = guidePolicyService;
  }

  @Override
  public ApiSearchResponse<ComponentDocument> searchComponents(
      String query,
      Integer offset,
      Integer limit,
      String sortField,
      String sortOrder,
      List<String> formats,
      List<String> categories,
      List<String> severities,
      Double minCvss,
      Double maxCvss,
      Double minEpss,
      Double maxEpss,
      List<String> licenseFamilies,
      List<String> licenses,
      Integer minVersionScore,
      Integer maxVersionScore,
      String latestStable,
      String publishedWindow,
      Boolean hasMalware,
      Integer minDocCount) throws IOException
  {
    return searchComponents(
        query, offset, limit, sortField, sortOrder, formats, categories, severities,
        minCvss, maxCvss, minEpss, maxEpss, licenseFamilies, licenses,
        minVersionScore, maxVersionScore, latestStable, publishedWindow,
        hasMalware, minDocCount, null, null);
  }

  @GET
  @Path("/search")
  @Override
  public ApiSearchResponse<ComponentDocument> searchComponents(
      @QueryParam("query") String query,
      @QueryParam("offset") Integer offset,
      @QueryParam("limit") Integer limit,
      @QueryParam("sortField") String sortField,
      @QueryParam("sortOrder") String sortOrder,
      @QueryParam("formats") List<String> formats,
      @QueryParam("categories") List<String> categories,
      @QueryParam("severities") List<String> severities,
      @QueryParam("minCvss") Double minCvss,
      @QueryParam("maxCvss") Double maxCvss,
      @QueryParam("minEpss") Double minEpss,
      @QueryParam("maxEpss") Double maxEpss,
      @QueryParam("licenseFamilies") List<String> licenseFamilies,
      @QueryParam("licenses") List<String> licenses,
      @QueryParam("minVersionScore") Integer minVersionScore,
      @QueryParam("maxVersionScore") Integer maxVersionScore,
      @QueryParam("latestStable") String latestStable,
      @QueryParam("publishedWindow") String publishedWindow,
      @QueryParam("hasMalware") Boolean hasMalware,
      @QueryParam("minDocCount") Integer minDocCount,
      @Parameter(description = "Restrict policy evaluation to this owner (application or organization "
          + "id). Omitted defaults to the root organization.") @QueryParam("ownerId") String ownerId,
      @Parameter(description = "Policy evaluation stage: develop, build, stage-release, release, or "
          + "operate. Case-insensitive; omitted defaults to release.") @QueryParam("stage") String stage) throws IOException
  {
    requireLimitWithinPolicyEnrichmentCap(limit);
    requireValidStage(stage);
    GuideComponentSearchRequest request = new GuideComponentSearchRequest(
        query, offset, limit, sortField, sortOrder, formats, categories, severities,
        minCvss, maxCvss, minEpss, maxEpss, licenseFamilies, licenses,
        minVersionScore, maxVersionScore, latestStable, publishedWindow,
        hasMalware, minDocCount);
    return guidePolicyService.enrichComponentSearch(searchApiClient.searchComponents(request), ownerId, stage);
  }

  @GET
  @Path("/detail")
  public ComponentDetailDocument getComponentDetail(
      @QueryParam("purl") String purl,
      @QueryParam("format") String format,
      @QueryParam("namespace") String namespace,
      @QueryParam("name") String name,
      @QueryParam("version") String version,
      @Parameter(description = "Restrict policy evaluation to this owner (application or organization "
          + "id). Omitted defaults to the root organization.") @QueryParam("ownerId") String ownerId,
      @Parameter(description = "Policy evaluation stage: develop, build, stage-release, release, or "
          + "operate. Case-insensitive; omitted defaults to release.") @QueryParam("stage") String stage,
      @Parameter(
          description = "Artifact extension (e.g. 'jar', 'whl'); omit for no filter.") @QueryParam("extension") String extension,
      @Parameter(
          description = "Artifact classifier (e.g. 'sources', 'javadoc'); omit for no filter.") @QueryParam("classifier") String classifier) throws IOException
  {
    String resolvedPurl = purl != null ? purl : GuidePurlAssembler.buildPurl(format, namespace, name, version);
    return resolveAndEnrichDetail(resolvedPurl, ownerId, stage, extension, classifier);
  }

  @Override
  public ComponentDetailDocument getComponentDetailByPurlQueryParam(String purl) throws IOException {
    return resolveAndEnrichDetail(purl, null, null, null, null);
  }

  @Override
  public ComponentDetailDocument getComponentDetailByQueryParams(
      String format,
      String namespace,
      String name,
      String version,
      String extension,
      String classifier) throws IOException
  {
    String purl = GuidePurlAssembler.buildPurl(format, namespace, name, version);
    return resolveAndEnrichDetail(purl, null, null, extension, classifier);
  }

  /**
   * Shared by the {@code /detail} wrapper and both interface-bound detail overrides.
   *
   * <p>
   * Note on qualifier application pattern: This method applies artifact qualifiers (extension/classifier)
   * to the PURL before calling {@code getComponentDetailByPurl}. In contrast, the versions/dependencies/
   * vulnerabilities endpoints store the raw PURL in request DTOs and apply qualifiers inside
   * {@code SearchApiClientImpl.buildComponent*Params}. The asymmetry is intentional—detail takes a pre-built
   * qualified PURL while the others build params from the raw PURL. Future maintainers should not add
   * qualifier application in both places to avoid double-encoding.
   */
  private ComponentDetailDocument resolveAndEnrichDetail(
      String purl,
      String ownerId,
      String stage,
      String extension,
      String classifier) throws IOException
  {
    requireValidStage(stage);
    GuidePurlValidator.validate(purl);
    String qualifiedPurl = PurlArtifactQualifiers.withArtifactQualifiers(purl, extension, classifier);
    return guidePolicyService.enrichComponentDetail(
        searchApiClient.getComponentDetailByPurl(qualifiedPurl), ownerId, stage);
  }

  @GET
  @Path("/versions")
  public ApiSearchResponse<ComponentDetailDocument> getComponentVersions(
      @QueryParam("purl") String purl,
      @QueryParam("format") String format,
      @QueryParam("namespace") String namespace,
      @QueryParam("name") String name,
      @QueryParam("version") String version,
      @QueryParam("offset") Integer offset,
      @QueryParam("limit") Integer limit,
      @QueryParam("sortField") String sortField,
      @QueryParam("sortOrder") String sortOrder,
      @QueryParam("severities") List<String> severities,
      @QueryParam("minCvss") Double minCvss,
      @QueryParam("maxCvss") Double maxCvss,
      @QueryParam("minVersionScore") Integer minVersionScore,
      @QueryParam("maxVersionScore") Integer maxVersionScore,
      @QueryParam("versionQuery") String versionQuery,
      @QueryParam("publishedWindow") String publishedWindow,
      @QueryParam("hasMalware") Boolean hasMalware,
      @QueryParam("isStable") Boolean isStable,
      @Parameter(description = "Restrict policy evaluation to this owner (application or organization "
          + "id). Omitted defaults to the root organization.") @QueryParam("ownerId") String ownerId,
      @Parameter(description = "Policy evaluation stage: develop, build, stage-release, release, or "
          + "operate. Case-insensitive; omitted defaults to release.") @QueryParam("stage") String stage,
      @Parameter(description = "Artifact extension.") @QueryParam("extension") String extension,
      @Parameter(description = "Artifact classifier.") @QueryParam("classifier") String classifier) throws IOException
  {
    requireLimitWithinPolicyEnrichmentCap(limit);
    String resolvedPurl = purl != null ? purl : GuidePurlAssembler.buildPurl(format, namespace, name, version);
    return resolveAndEnrichVersions(
        resolvedPurl, extension, classifier, offset, limit, sortField, sortOrder, severities, minCvss, maxCvss,
        minVersionScore, maxVersionScore, versionQuery, publishedWindow, hasMalware, isStable, ownerId, stage);
  }

  @Override
  public ApiSearchResponse<ComponentDetailDocument> getComponentVersionsByPurlQueryParam(
      String purl,
      Integer offset,
      Integer limit,
      String sortField,
      String sortOrder,
      List<String> severities,
      Double minCvss,
      Double maxCvss,
      Integer minVersionScore,
      Integer maxVersionScore,
      String versionQuery,
      String publishedWindow,
      Boolean hasMalware,
      Boolean isStable) throws IOException
  {
    return resolveAndEnrichVersions(
        purl, null, null, offset, limit, sortField, sortOrder, severities, minCvss, maxCvss, minVersionScore,
        maxVersionScore, versionQuery, publishedWindow, hasMalware, isStable, null, null);
  }

  @Override
  public ApiSearchResponse<ComponentDetailDocument> getComponentVersionsByQueryParams(
      String format,
      String namespace,
      String name,
      String version,
      Integer offset,
      Integer limit,
      String sortField,
      String sortOrder,
      List<String> severities,
      Double minCvss,
      Double maxCvss,
      Integer minVersionScore,
      Integer maxVersionScore,
      String versionQuery,
      String publishedWindow,
      Boolean hasMalware,
      Boolean isStable,
      String extension,
      String classifier) throws IOException
  {
    String purl = GuidePurlAssembler.buildPurl(format, namespace, name, version);
    return resolveAndEnrichVersions(
        purl, extension, classifier, offset, limit, sortField, sortOrder, severities, minCvss, maxCvss,
        minVersionScore, maxVersionScore, versionQuery, publishedWindow, hasMalware, isStable, null, null);
  }

  /** Shared by the {@code /versions} wrapper and both interface-bound versions overrides. */
  private ApiSearchResponse<ComponentDetailDocument> resolveAndEnrichVersions(
      String purl,
      String extension,
      String classifier,
      Integer offset,
      Integer limit,
      String sortField,
      String sortOrder,
      List<String> severities,
      Double minCvss,
      Double maxCvss,
      Integer minVersionScore,
      Integer maxVersionScore,
      String versionQuery,
      String publishedWindow,
      Boolean hasMalware,
      Boolean isStable,
      String ownerId,
      String stage) throws IOException
  {
    requireValidStage(stage);
    GuidePurlValidator.validate(purl);
    GuideComponentVersionsRequest request = new GuideComponentVersionsRequest(
        purl, extension, classifier, offset, limit, sortField, sortOrder, severities, minCvss, maxCvss,
        minVersionScore, maxVersionScore, versionQuery, publishedWindow, hasMalware, isStable);
    return guidePolicyService.enrichComponentDetailSearch(
        searchApiClient.getComponentVersions(request), ownerId, stage);
  }

  @GET
  @Path("/vulnerabilities")
  public ApiSearchResponse<VulnerabilityDocument> getComponentVulnerabilities(
      @QueryParam("purl") String purl,
      @QueryParam("format") String format,
      @QueryParam("namespace") String namespace,
      @QueryParam("name") String name,
      @QueryParam("version") String version,
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
      @Parameter(description = "Artifact extension.") @QueryParam("extension") String extension,
      @Parameter(description = "Artifact classifier.") @QueryParam("classifier") String classifier) throws IOException
  {
    String resolvedPurl = purl != null ? purl : GuidePurlAssembler.buildPurl(format, namespace, name, version);
    return resolveVulnerabilities(
        resolvedPurl, extension, classifier, offset, limit, sortField, sortOrder, severities, minCvss, maxCvss,
        minEpss, maxEpss, hasMalware, patchAvailable, policyCompliant, cwes, exploitationKnown, publishedWindow);
  }

  @Override
  public ApiSearchResponse<VulnerabilityDocument> getComponentVulnerabilitiesByPurlQueryParam(
      String purl,
      Integer offset,
      Integer limit,
      String sortField,
      String sortOrder,
      List<String> severities,
      Double minCvss,
      Double maxCvss,
      Double minEpss,
      Double maxEpss,
      Boolean hasMalware,
      Boolean patchAvailable,
      Boolean policyCompliant,
      List<String> cwes,
      Boolean exploitationKnown,
      String publishedWindow) throws IOException
  {
    return resolveVulnerabilities(
        purl, null, null, offset, limit, sortField, sortOrder, severities, minCvss, maxCvss,
        minEpss, maxEpss, hasMalware, patchAvailable, policyCompliant, cwes, exploitationKnown, publishedWindow);
  }

  @Override
  public ApiSearchResponse<VulnerabilityDocument> getComponentVulnerabilitiesByQueryParams(
      String format,
      String namespace,
      String name,
      String version,
      Integer offset,
      Integer limit,
      String sortField,
      String sortOrder,
      List<String> severities,
      Double minCvss,
      Double maxCvss,
      Double minEpss,
      Double maxEpss,
      Boolean hasMalware,
      Boolean patchAvailable,
      Boolean policyCompliant,
      List<String> cwes,
      Boolean exploitationKnown,
      String publishedWindow,
      String extension,
      String classifier) throws IOException
  {
    String purl = GuidePurlAssembler.buildPurl(format, namespace, name, version);
    return resolveVulnerabilities(
        purl, extension, classifier, offset, limit, sortField, sortOrder, severities, minCvss, maxCvss,
        minEpss, maxEpss, hasMalware, patchAvailable, policyCompliant, cwes, exploitationKnown, publishedWindow);
  }

  /**
   * Shared helper for the three vulnerability entry points.
   *
   * @param policyCompliant Accepted for API compatibility but ignored. Policy evaluation is a
   *          Lifecycle-specific feature; this parameter is silently dropped rather than rejecting with
   *          a 400 error to maintain backward compatibility with clients that include it.
   */
  private ApiSearchResponse<VulnerabilityDocument> resolveVulnerabilities(
      String purl,
      String extension,
      String classifier,
      Integer offset,
      Integer limit,
      String sortField,
      String sortOrder,
      List<String> severities,
      Double minCvss,
      Double maxCvss,
      Double minEpss,
      Double maxEpss,
      Boolean hasMalware,
      Boolean patchAvailable,
      Boolean policyCompliant,
      List<String> cwes,
      Boolean exploitationKnown,
      String publishedWindow) throws IOException
  {
    GuidePurlValidator.validate(purl);
    GuideComponentVulnerabilitiesRequest request = new GuideComponentVulnerabilitiesRequest(
        purl, extension, classifier, null, null, null, null, offset, limit, sortField, sortOrder,
        severities, minCvss, maxCvss, minEpss, maxEpss, hasMalware,
        patchAvailable, cwes, exploitationKnown, publishedWindow);
    return searchApiClient.getComponentVulnerabilities(request);
  }

  @GET
  @Path("/dependencies")
  public ApiSearchResponse<ComponentDocument> getComponentDependencies(
      @QueryParam("purl") String purl,
      @QueryParam("format") String format,
      @QueryParam("namespace") String namespace,
      @QueryParam("name") String name,
      @QueryParam("version") String version,
      @QueryParam("query") String query,
      @QueryParam("offset") Integer offset,
      @QueryParam("limit") Integer limit,
      @QueryParam("sortField") String sortField,
      @QueryParam("sortOrder") String sortOrder,
      @QueryParam("formats") List<String> formats,
      @QueryParam("categories") List<String> categories,
      @QueryParam("severities") List<String> severities,
      @QueryParam("minCvss") Double minCvss,
      @QueryParam("maxCvss") Double maxCvss,
      @QueryParam("minVersionScore") Integer minVersionScore,
      @QueryParam("maxVersionScore") Integer maxVersionScore,
      @QueryParam("licenseFamilies") List<String> licenseFamilies,
      @QueryParam("licenses") List<String> licenses,
      @QueryParam("latestStable") String latestStable,
      @Parameter(description = "Restrict policy evaluation to this owner (application or organization "
          + "id). Omitted defaults to the root organization.") @QueryParam("ownerId") String ownerId,
      @Parameter(description = "Policy evaluation stage: develop, build, stage-release, release, or "
          + "operate. Case-insensitive; omitted defaults to release.") @QueryParam("stage") String stage,
      @Parameter(description = "Artifact extension.") @QueryParam("extension") String extension,
      @Parameter(description = "Artifact classifier.") @QueryParam("classifier") String classifier) throws IOException
  {
    requireLimitWithinPolicyEnrichmentCap(limit);
    String resolvedPurl = purl != null ? purl : GuidePurlAssembler.buildPurl(format, namespace, name, version);
    return resolveAndEnrichDependencies(
        resolvedPurl, extension, classifier, query, offset, limit, sortField, sortOrder, formats, categories,
        severities,
        minCvss, maxCvss, minVersionScore, maxVersionScore, licenseFamilies, licenses, latestStable, ownerId, stage);
  }

  @Override
  public ApiSearchResponse<ComponentDocument> getComponentDependenciesByPurlQueryParam(
      String purl,
      String query,
      Integer offset,
      Integer limit,
      String sortField,
      String sortOrder,
      List<String> formats,
      List<String> categories,
      List<String> severities,
      Double minCvss,
      Double maxCvss,
      Integer minVersionScore,
      Integer maxVersionScore,
      List<String> licenseFamilies,
      List<String> licenses,
      String latestStable) throws IOException
  {
    return resolveAndEnrichDependencies(
        purl, null, null, query, offset, limit, sortField, sortOrder, formats, categories, severities,
        minCvss, maxCvss, minVersionScore, maxVersionScore, licenseFamilies, licenses, latestStable, null, null);
  }

  @Override
  public ApiSearchResponse<ComponentDocument> getComponentDependenciesByQueryParams(
      String format,
      String namespace,
      String name,
      String version,
      String query,
      Integer offset,
      Integer limit,
      String sortField,
      String sortOrder,
      List<String> formats,
      List<String> categories,
      List<String> severities,
      Double minCvss,
      Double maxCvss,
      Integer minVersionScore,
      Integer maxVersionScore,
      List<String> licenseFamilies,
      List<String> licenses,
      String latestStable,
      String extension,
      String classifier) throws IOException
  {
    String purl = GuidePurlAssembler.buildPurl(format, namespace, name, version);
    return resolveAndEnrichDependencies(
        purl, extension, classifier, query, offset, limit, sortField, sortOrder, formats, categories, severities,
        minCvss, maxCvss, minVersionScore, maxVersionScore, licenseFamilies, licenses, latestStable, null, null);
  }

  /** Shared by the {@code /dependencies} wrapper and both interface-bound dependencies overrides. */
  private ApiSearchResponse<ComponentDocument> resolveAndEnrichDependencies(
      String purl,
      String extension,
      String classifier,
      String query,
      Integer offset,
      Integer limit,
      String sortField,
      String sortOrder,
      List<String> formats,
      List<String> categories,
      List<String> severities,
      Double minCvss,
      Double maxCvss,
      Integer minVersionScore,
      Integer maxVersionScore,
      List<String> licenseFamilies,
      List<String> licenses,
      String latestStable,
      String ownerId,
      String stage) throws IOException
  {
    requireValidStage(stage);
    GuidePurlValidator.validate(purl);
    GuideComponentDependenciesRequest request = new GuideComponentDependenciesRequest(
        purl, extension, classifier, null, null, null, null, query, offset, limit, sortField, sortOrder,
        formats, categories, severities, minCvss, maxCvss,
        minVersionScore, maxVersionScore, licenseFamilies, licenses, latestStable);
    return guidePolicyService.enrichComponentSearch(
        searchApiClient.getComponentDependencies(request), ownerId, stage);
  }

  @Override
  public ComponentDetailDocument getLatestVersion(LatestVersionRequest request) throws IOException {
    return getLatestVersion(request, null, null);
  }

  @POST
  @Path("/latest-version")
  @Consumes(MediaType.APPLICATION_JSON)
  @Override
  public ComponentDetailDocument getLatestVersion(
      LatestVersionRequest request,
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
    requireValidStage(stage);
    GuidePurlValidator.validate(request.purl());
    return guidePolicyService.enrichComponentDetail(
        searchApiClient.getLatestVersionDetail(request.purl()), ownerId, stage);
  }

}
