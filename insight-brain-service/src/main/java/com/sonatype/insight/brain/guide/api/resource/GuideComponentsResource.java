/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.resource;

import java.io.IOException;
import java.util.List;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
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
import com.sonatype.insight.brain.guide.core.SearchApiClient;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
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
@ProductLicenseEnforcementPoint(LicensedFeature.GUIDE_SEARCH)
public class GuideComponentsResource
    implements GuideComponentsApi
{

  private final SearchApiClient searchApiClient;

  @Inject
  public GuideComponentsResource(SearchApiClient searchApiClient) {
    this.searchApiClient = searchApiClient;
  }

  @GET
  @Path("/search")
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
      @QueryParam("minDocCount") Integer minDocCount) throws IOException
  {
    GuideComponentSearchRequest request = new GuideComponentSearchRequest(
        query, offset, limit, sortField, sortOrder, formats, categories, severities,
        minCvss, maxCvss, minEpss, maxEpss, licenseFamilies, licenses,
        minVersionScore, maxVersionScore, latestStable, publishedWindow,
        hasMalware, minDocCount);
    return searchApiClient.searchComponents(request);
  }

  @GET
  @Path("/detail")
  public ComponentDetailDocument getComponentDetail(
      @QueryParam("purl") String purl,
      @QueryParam("format") String format,
      @QueryParam("namespace") String namespace,
      @QueryParam("name") String name,
      @QueryParam("version") String version) throws IOException
  {
    if (purl != null) {
      return getComponentDetailByPurlQueryParam(purl);
    }
    return getComponentDetailByQueryParams(format, namespace, name, version);
  }

  @Override
  public ComponentDetailDocument getComponentDetailByPurlQueryParam(String purl) throws IOException {
    GuidePurlValidator.validate(purl);
    return searchApiClient.getComponentDetailByPurl(purl);
  }

  @Override
  public ComponentDetailDocument getComponentDetailByQueryParams(
      String format,
      String namespace,
      String name,
      String version) throws IOException
  {
    // Build PURL from query params and delegate
    String purl = buildPurl(format, namespace, name, version);
    return getComponentDetailByPurlQueryParam(purl);
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
      @QueryParam("isStable") Boolean isStable) throws IOException
  {
    if (purl != null) {
      return getComponentVersionsByPurlQueryParam(
          purl, offset, limit, sortField, sortOrder, severities, minCvss, maxCvss, minVersionScore, maxVersionScore,
          versionQuery, publishedWindow, hasMalware, isStable);
    }
    return getComponentVersionsByQueryParams(
        format, namespace, name, version, offset, limit, sortField, sortOrder, severities, minCvss, maxCvss,
        minVersionScore, maxVersionScore, versionQuery, publishedWindow, hasMalware, isStable);
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
    GuidePurlValidator.validate(purl);
    GuideComponentVersionsRequest request = new GuideComponentVersionsRequest(
        purl, offset, limit, sortField, sortOrder, severities, minCvss, maxCvss,
        minVersionScore, maxVersionScore, versionQuery, publishedWindow, hasMalware, isStable);
    return searchApiClient.getComponentVersions(request);
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
      Boolean isStable) throws IOException
  {
    String purl = buildPurl(format, namespace, name, version);
    return getComponentVersionsByPurlQueryParam(
        purl, offset, limit, sortField, sortOrder, severities, minCvss, maxCvss,
        minVersionScore, maxVersionScore, versionQuery, publishedWindow, hasMalware, isStable);
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
      @QueryParam("publishedWindow") String publishedWindow) throws IOException
  {
    if (purl != null) {
      return getComponentVulnerabilitiesByPurlQueryParam(
          purl, offset, limit, sortField, sortOrder, severities, minCvss, maxCvss, minEpss, maxEpss, hasMalware,
          patchAvailable, policyCompliant, cwes, exploitationKnown, publishedWindow);
    }
    return getComponentVulnerabilitiesByQueryParams(
        format, namespace, name, version, offset, limit, sortField, sortOrder, severities, minCvss, maxCvss, minEpss,
        maxEpss, hasMalware, patchAvailable, policyCompliant, cwes, exploitationKnown, publishedWindow);
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
    GuidePurlValidator.validate(purl);
    // policyCompliant is not supported - policy evaluation is a Lifecycle-specific feature.
    GuideComponentVulnerabilitiesRequest request = new GuideComponentVulnerabilitiesRequest(
        purl, null, null, null, null, offset, limit, sortField, sortOrder,
        severities, minCvss, maxCvss, minEpss, maxEpss, hasMalware,
        patchAvailable, cwes, exploitationKnown, publishedWindow);
    return searchApiClient.getComponentVulnerabilities(request);
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
      String publishedWindow) throws IOException
  {
    String purl = buildPurl(format, namespace, name, version);
    return getComponentVulnerabilitiesByPurlQueryParam(
        purl, offset, limit, sortField, sortOrder, severities, minCvss, maxCvss, minEpss, maxEpss,
        hasMalware, patchAvailable, policyCompliant, cwes, exploitationKnown, publishedWindow);
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
      @QueryParam("latestStable") String latestStable) throws IOException
  {
    if (purl != null) {
      return getComponentDependenciesByPurlQueryParam(
          purl, query, offset, limit, sortField, sortOrder, formats, categories, severities, minCvss, maxCvss,
          minVersionScore, maxVersionScore, licenseFamilies, licenses, latestStable);
    }
    return getComponentDependenciesByQueryParams(
        format, namespace, name, version, query, offset, limit, sortField, sortOrder, formats, categories, severities,
        minCvss, maxCvss, minVersionScore, maxVersionScore, licenseFamilies, licenses, latestStable);
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
    GuidePurlValidator.validate(purl);
    GuideComponentDependenciesRequest request = new GuideComponentDependenciesRequest(
        purl, null, null, null, null, query, offset, limit, sortField, sortOrder,
        formats, categories, severities, minCvss, maxCvss,
        minVersionScore, maxVersionScore, licenseFamilies, licenses, latestStable);
    return searchApiClient.getComponentDependencies(request);
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
      String latestStable) throws IOException
  {
    String purl = buildPurl(format, namespace, name, version);
    return getComponentDependenciesByPurlQueryParam(
        purl, query, offset, limit, sortField, sortOrder, formats, categories, severities, minCvss, maxCvss,
        minVersionScore, maxVersionScore, licenseFamilies, licenses, latestStable);
  }

  @POST
  @Path("/latest-version")
  @Consumes(MediaType.APPLICATION_JSON)
  @Override
  public ComponentDetailDocument getLatestVersion(LatestVersionRequest request) throws IOException {
    // request itself is null when JAX-RS receives an empty or JSON-`null` body. Without this
    // guard the next line NPEs and Dropwizard's default handler returns a non-Guide envelope.
    if (request == null || request.purl() == null || request.purl().isBlank()) {
      throw new GuideApiException(Response.Status.BAD_REQUEST, "Purl is required");
    }
    GuidePurlValidator.validate(request.purl());
    return searchApiClient.getLatestVersionDetail(request.purl());
  }

  /**
   * Builds a PURL string from individual coordinate query parameters. Used by the by-coords
   * branch of every endpoint that accepts either {@code purl} or {@code format/namespace/name/
   * version}.
   *
   * <p>
   * Requires {@code format}, {@code name}, AND {@code version} to all be present. {@code
   * namespace} is optional (some ecosystems like npm and pypi don't have one). Guide SaaS's
   * Spring routing rejects the request at dispatch time when any of these three are missing
   * (the @RequestMapping {@code params="format,name,version"} predicate doesn't match), so
   * IQ self-hosted enforces the same contract here. Without this validation, IQ would
   * silently build a versionless PURL like {@code pkg:maven/log4j-core} and forward it to
   * the upstream search-server, which would either return spurious results or error out as
   * a 500 — both contract violations vs SaaS.
   *
   * <p>
   * Delegates assembly to {@link PackageURL}'s typed constructor, mirroring SaaS's
   * {@code EntityIdExtractor.buildPurlFromCoordinates}. The constructor URL-encodes each
   * coordinate component automatically, so values containing characters like {@code /}, {@code
   * @}, or {@code %} are encoded rather than mis-interpreted as PURL syntax — e.g. {@code
   * name=log4j/core} becomes {@code pkg:maven/log4j%2Fcore@1.0} (a single name with an encoded
   * slash) instead of {@code pkg:maven/log4j/core@1.0} (which the parser would treat as
   * namespace=log4j, name=core).
   */
  private String buildPurl(String format, String namespace, String name, String version) {
    if (format == null || format.isBlank()
        || name == null || name.isBlank()
        || version == null || version.isBlank())
    {
      throw new GuideApiException(Response.Status.BAD_REQUEST,
          "Either 'purl' or all of 'format', 'name', 'version' are required");
    }
    String namespaceOrNull = (namespace == null || namespace.isEmpty()) ? null : namespace;
    try {
      return new PackageURL(format, namespaceOrNull, name, version, null, null).canonicalize();
    }
    catch (MalformedPackageURLException e) {
      throw new GuideApiException(Response.Status.BAD_REQUEST, "Invalid PURL coordinates: " + e.getMessage());
    }
  }
}
