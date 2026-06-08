/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.core;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sonatype.guide.api.dto.AffectedComponentVersion;
import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.guide.api.dto.ComponentDetailDocument;
import com.sonatype.guide.api.dto.ComponentDocument;
import com.sonatype.guide.api.dto.SearchResult;
import com.sonatype.guide.api.dto.VulnerabilityDetailDocument;
import com.sonatype.guide.api.dto.VulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersionRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersionSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDependenciesRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDetailDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDetailSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentSearchRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentVersionsRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentVulnerabilitiesRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideGlobalSearchRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideGlobalSearchResponse;
import com.sonatype.insight.brain.guide.api.dto.GuideRecommendationResult;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDetailDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchResponse;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.api.error.GuideNotFoundException;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Response;

@Named
@Singleton
public class SearchApiClientImpl
    implements SearchApiClient
{
  private final HdsClient hdsClient;

  @Inject
  public SearchApiClientImpl(HdsClient hdsClient) {
    this.hdsClient = hdsClient;
  }

  @Authorize(permission = Permission.READ)
  @Override
  public String getComponentByPurl(String purl) {
    return hdsClient.get(String.class, "rest/search/components/detail", Map.of("purl", purlForUpstream(purl)));
  }

  @Authorize(permission = Permission.READ)
  @Override
  public String getLatestComponentVersion(String purl) {
    return hdsClient.post(String.class, "rest/search/components/latest-version",
        Map.of("purl", purlForUpstream(purl)));
  }

  @Authorize(permission = Permission.READ)
  @Override
  public GuideRecommendationResult getRecommendations(String purl) {
    try {
      return hdsClient.post(GuideRecommendationResult.class, "rest/search/recommendations",
          Map.of("purl", purlForUpstream(purl)));
    }
    catch (NotFoundException e) {
      throw notFound(e, "No recommendations found for purl: " + purl);
    }
    catch (BadGatewayException | InternalServerErrorException e) {
      throw new GuideApiException(Response.Status.BAD_GATEWAY, "Failed to retrieve recommendations from data service");
    }
  }

  @Authorize(permission = Permission.READ)
  @Override
  public ApiSearchResponse<ComponentDocument> searchComponents(GuideComponentSearchRequest request) {
    try {
      return hdsClient.getWithMultimap(
          GuideComponentSearchResponse.class, "rest/search/components", buildComponentSearchParams(request));
    }
    catch (NotFoundException e) {
      int limit = request.limit() != null ? request.limit() : 20;
      int offset = request.offset() != null ? request.offset() : 0;
      return new GuideComponentSearchResponse(List.of(), 0, offset, limit, null);
    }
    catch (BadGatewayException | InternalServerErrorException e) {
      throw new GuideApiException(Response.Status.BAD_GATEWAY,
          "Failed to retrieve component search results from data service");
    }
  }

  private static Multimap<String, String> buildComponentSearchParams(GuideComponentSearchRequest request) {
    Multimap<String, String> params = ArrayListMultimap.create();
    if (request.query() != null) {
      params.put("query", request.query());
    }
    if (request.offset() != null) {
      params.put("offset", String.valueOf(request.offset()));
    }
    if (request.limit() != null) {
      params.put("limit", String.valueOf(request.limit()));
    }
    if (request.sortField() != null) {
      params.put("sortField", request.sortField());
    }
    if (request.sortOrder() != null) {
      params.put("sortOrder", request.sortOrder());
    }
    if (request.formats() != null) {
      request.formats().forEach(f -> params.put("formats", f));
    }
    if (request.categories() != null) {
      request.categories().forEach(c -> params.put("categories", c));
    }
    if (request.severities() != null) {
      request.severities().forEach(s -> params.put("severities", s));
    }
    if (request.minCvss() != null) {
      params.put("minCvss", String.valueOf(request.minCvss()));
    }
    if (request.maxCvss() != null) {
      params.put("maxCvss", String.valueOf(request.maxCvss()));
    }
    if (request.minEpss() != null) {
      params.put("minEpss", String.valueOf(request.minEpss()));
    }
    if (request.maxEpss() != null) {
      params.put("maxEpss", String.valueOf(request.maxEpss()));
    }
    if (request.licenseFamilies() != null) {
      request.licenseFamilies().forEach(l -> params.put("licenseFamilies", l));
    }
    if (request.licenses() != null) {
      request.licenses().forEach(l -> params.put("licenses", l));
    }
    if (request.minVersionScore() != null) {
      params.put("minVersionScore", String.valueOf(request.minVersionScore()));
    }
    if (request.maxVersionScore() != null) {
      params.put("maxVersionScore", String.valueOf(request.maxVersionScore()));
    }
    if (request.latestStable() != null) {
      params.put("latestStable", request.latestStable());
    }
    if (request.publishedWindow() != null) {
      params.put("publishedWindow", request.publishedWindow());
    }
    if (request.hasMalware() != null) {
      params.put("hasMalware", String.valueOf(request.hasMalware()));
    }
    if (request.minDocCount() != null) {
      params.put("minDocCount", String.valueOf(request.minDocCount()));
    }
    return params;
  }

  @Authorize(permission = Permission.READ)
  @Override
  public ApiSearchResponse<VulnerabilityDocument> searchVulnerabilities(GuideVulnerabilitySearchRequest request) {
    try {
      return hdsClient.getWithMultimap(
          GuideVulnerabilitySearchResponse.class, "rest/search/vulnerabilities",
          buildVulnerabilitySearchParams(request));
    }
    catch (NotFoundException e) {
      int limit = request.limit() != null ? request.limit() : 20;
      int offset = request.offset() != null ? request.offset() : 0;
      return new GuideVulnerabilitySearchResponse(List.of(), 0, offset, limit, null);
    }
    catch (BadGatewayException | InternalServerErrorException e) {
      throw new GuideApiException(Response.Status.BAD_GATEWAY,
          "Failed to retrieve vulnerability search results from data service");
    }
  }

  private static Multimap<String, String> buildVulnerabilitySearchParams(GuideVulnerabilitySearchRequest request) {
    Multimap<String, String> params = ArrayListMultimap.create();
    if (request.query() != null) {
      params.put("query", request.query());
    }
    if (request.offset() != null) {
      params.put("offset", String.valueOf(request.offset()));
    }
    if (request.limit() != null) {
      params.put("limit", String.valueOf(request.limit()));
    }
    if (request.sortField() != null) {
      params.put("sortField", request.sortField());
    }
    if (request.sortOrder() != null) {
      params.put("sortOrder", request.sortOrder());
    }
    if (request.severities() != null) {
      request.severities().forEach(s -> params.put("severities", s));
    }
    if (request.minCvss() != null) {
      params.put("minCvss", String.valueOf(request.minCvss()));
    }
    if (request.maxCvss() != null) {
      params.put("maxCvss", String.valueOf(request.maxCvss()));
    }
    if (request.minEpss() != null) {
      params.put("minEpss", String.valueOf(request.minEpss()));
    }
    if (request.maxEpss() != null) {
      params.put("maxEpss", String.valueOf(request.maxEpss()));
    }
    if (request.hasMalware() != null) {
      params.put("hasMalware", String.valueOf(request.hasMalware()));
    }
    if (request.patchAvailable() != null) {
      params.put("patchAvailable", String.valueOf(request.patchAvailable()));
    }
    if (request.cwes() != null) {
      request.cwes().forEach(c -> params.put("cwes", c));
    }
    if (request.exploitationKnown() != null) {
      params.put("exploitationKnown", String.valueOf(request.exploitationKnown()));
    }
    if (request.publishedWindow() != null) {
      params.put("publishedWindow", request.publishedWindow());
    }
    if (request.affectedEcosystems() != null) {
      request.affectedEcosystems().forEach(e -> params.put("affectedEcosystems", e));
    }
    if (request.minDocCount() != null) {
      params.put("minDocCount", String.valueOf(request.minDocCount()));
    }
    return params;
  }

  @Authorize(permission = Permission.READ)
  @Override
  public ApiSearchResponse<SearchResult> globalSearch(GuideGlobalSearchRequest request) {
    try {
      return hdsClient.getWithMultimap(
          GuideGlobalSearchResponse.class, "rest/search/global", buildGlobalSearchParams(request));
    }
    catch (NotFoundException e) {
      int limit = request.limit() != null ? request.limit() : 20;
      int offset = request.offset() != null ? request.offset() : 0;
      return new GuideGlobalSearchResponse(List.of(), 0, offset, limit, null);
    }
    catch (BadGatewayException | InternalServerErrorException e) {
      throw new GuideApiException(Response.Status.BAD_GATEWAY,
          "Failed to retrieve global search results from data service");
    }
  }

  private static Multimap<String, String> buildGlobalSearchParams(GuideGlobalSearchRequest request) {
    Multimap<String, String> params = ArrayListMultimap.create();
    if (request.query() != null) {
      params.put("query", request.query());
    }
    if (request.offset() != null) {
      params.put("offset", String.valueOf(request.offset()));
    }
    if (request.limit() != null) {
      params.put("limit", String.valueOf(request.limit()));
    }
    if (request.sortField() != null) {
      params.put("sortField", request.sortField());
    }
    if (request.sortOrder() != null) {
      params.put("sortOrder", request.sortOrder());
    }
    if (request.latestStable() != null) {
      params.put("latestStable", request.latestStable());
    }
    if (request.formats() != null) {
      request.formats().forEach(f -> params.put("formats", f));
    }
    if (request.publishedWindow() != null) {
      params.put("publishedWindow", request.publishedWindow());
    }
    return params;
  }

  @Authorize(permission = Permission.READ)
  @Override
  public VulnerabilityDetailDocument getVulnerabilityByRefId(String id) {
    try {
      return hdsClient.get(GuideVulnerabilityDetailDocument.class,
          "rest/search/vulnerabilities/" + encodePathSegment(id));
    }
    catch (NotFoundException e) {
      throw notFound(e, "Vulnerability not found: " + id);
    }
    catch (BadGatewayException | InternalServerErrorException e) {
      throw new GuideApiException(Response.Status.BAD_GATEWAY,
          "Failed to retrieve vulnerability detail from data service");
    }
  }

  @Authorize(permission = Permission.READ)
  @Override
  public ApiSearchResponse<AffectedComponentVersion> getVulnerabilityAffectedComponents(
      GuideAffectedComponentVersionRequest request)
  {
    try {
      return hdsClient.getWithMultimap(
          GuideAffectedComponentVersionSearchResponse.class,
          "rest/search/vulnerabilities/" + encodePathSegment(request.id()) + "/components",
          buildAffectedComponentParams(request));
    }
    catch (NotFoundException e) {
      int limit = request.limit() != null ? request.limit() : 20;
      int offset = request.offset() != null ? request.offset() : 0;
      return new GuideAffectedComponentVersionSearchResponse(List.of(), 0, offset, limit, null);
    }
    catch (BadGatewayException | InternalServerErrorException e) {
      throw new GuideApiException(Response.Status.BAD_GATEWAY,
          "Failed to retrieve vulnerability affected components from data service");
    }
  }

  private static Multimap<String, String> buildAffectedComponentParams(
      GuideAffectedComponentVersionRequest request)
  {
    Multimap<String, String> params = ArrayListMultimap.create();
    if (request.query() != null) {
      params.put("query", request.query());
    }
    if (request.offset() != null) {
      params.put("offset", String.valueOf(request.offset()));
    }
    if (request.limit() != null) {
      params.put("limit", String.valueOf(request.limit()));
    }
    if (request.sortField() != null) {
      params.put("sortField", request.sortField());
    }
    if (request.sortOrder() != null) {
      params.put("sortOrder", request.sortOrder());
    }
    return params;
  }

  @Authorize(permission = Permission.READ)
  @Override
  public ComponentDetailDocument getComponentDetailByPurl(String purl) {
    try {
      return hdsClient.get(
          GuideComponentDetailDocument.class,
          "rest/search/components/detail",
          Map.of("purl", purlForUpstream(purl)));
    }
    catch (NotFoundException e) {
      throw notFound(e, "Component not found: " + purl);
    }
    catch (BadGatewayException | InternalServerErrorException e) {
      throw new GuideApiException(Response.Status.BAD_GATEWAY,
          "Failed to retrieve component detail from data service");
    }
  }

  @Authorize(permission = Permission.READ)
  @Override
  public ApiSearchResponse<ComponentDetailDocument> getComponentVersions(
      GuideComponentVersionsRequest request)
  {
    try {
      return hdsClient.getWithMultimap(
          GuideComponentDetailSearchResponse.class,
          "rest/search/components/versions",
          buildComponentVersionsParams(request));
    }
    catch (NotFoundException e) {
      int limit = request.limit() != null ? request.limit() : 20;
      int offset = request.offset() != null ? request.offset() : 0;
      return new GuideComponentDetailSearchResponse(List.of(), 0, offset, limit, null);
    }
    catch (BadGatewayException | InternalServerErrorException e) {
      throw new GuideApiException(Response.Status.BAD_GATEWAY,
          "Failed to retrieve component versions from data service");
    }
  }

  private static Multimap<String, String> buildComponentVersionsParams(
      GuideComponentVersionsRequest request)
  {
    Multimap<String, String> params = ArrayListMultimap.create();
    if (request.purl() != null) {
      params.put("purl", purlForUpstream(request.purl()));
    }
    if (request.offset() != null) {
      params.put("offset", String.valueOf(request.offset()));
    }
    if (request.limit() != null) {
      params.put("limit", String.valueOf(request.limit()));
    }
    if (request.sortField() != null) {
      params.put("sortField", request.sortField());
    }
    if (request.sortOrder() != null) {
      params.put("sortOrder", request.sortOrder());
    }
    if (request.severities() != null) {
      request.severities().forEach(s -> params.put("severities", s));
    }
    if (request.minCvss() != null) {
      params.put("minCvss", String.valueOf(request.minCvss()));
    }
    if (request.maxCvss() != null) {
      params.put("maxCvss", String.valueOf(request.maxCvss()));
    }
    if (request.minVersionScore() != null) {
      params.put("minVersionScore", String.valueOf(request.minVersionScore()));
    }
    if (request.maxVersionScore() != null) {
      params.put("maxVersionScore", String.valueOf(request.maxVersionScore()));
    }
    if (request.versionQuery() != null) {
      params.put("versionQuery", request.versionQuery());
    }
    if (request.publishedWindow() != null) {
      params.put("publishedWindow", request.publishedWindow());
    }
    if (request.hasMalware() != null) {
      params.put("hasMalware", String.valueOf(request.hasMalware()));
    }
    if (request.isStable() != null) {
      params.put("isStable", String.valueOf(request.isStable()));
    }
    return params;
  }

  @Authorize(permission = Permission.READ)
  @Override
  public ApiSearchResponse<VulnerabilityDocument> getComponentVulnerabilities(
      GuideComponentVulnerabilitiesRequest request)
  {
    try {
      return hdsClient.getWithMultimap(
          GuideVulnerabilitySearchResponse.class,
          "rest/search/components/vulnerabilities",
          buildComponentVulnerabilitiesParams(request));
    }
    catch (NotFoundException e) {
      int limit = request.limit() != null ? request.limit() : 20;
      int offset = request.offset() != null ? request.offset() : 0;
      return new GuideVulnerabilitySearchResponse(List.of(), 0, offset, limit, null);
    }
    catch (BadGatewayException | InternalServerErrorException e) {
      throw new GuideApiException(Response.Status.BAD_GATEWAY,
          "Failed to retrieve component vulnerabilities from data service");
    }
  }

  private static Multimap<String, String> buildComponentVulnerabilitiesParams(
      GuideComponentVulnerabilitiesRequest request)
  {
    Multimap<String, String> params = ArrayListMultimap.create();
    if (request.purl() != null) {
      params.put("purl", purlForUpstream(request.purl()));
    }
    if (request.format() != null) {
      params.put("format", request.format());
    }
    if (request.namespace() != null) {
      params.put("namespace", request.namespace());
    }
    if (request.name() != null) {
      params.put("name", request.name());
    }
    if (request.version() != null) {
      params.put("version", request.version());
    }
    if (request.offset() != null) {
      params.put("offset", String.valueOf(request.offset()));
    }
    if (request.limit() != null) {
      params.put("limit", String.valueOf(request.limit()));
    }
    if (request.sortField() != null) {
      params.put("sortField", request.sortField());
    }
    if (request.sortOrder() != null) {
      params.put("sortOrder", request.sortOrder());
    }
    if (request.severities() != null) {
      request.severities().forEach(s -> params.put("severities", s));
    }
    if (request.minCvss() != null) {
      params.put("minCvss", String.valueOf(request.minCvss()));
    }
    if (request.maxCvss() != null) {
      params.put("maxCvss", String.valueOf(request.maxCvss()));
    }
    if (request.minEpss() != null) {
      params.put("minEpss", String.valueOf(request.minEpss()));
    }
    if (request.maxEpss() != null) {
      params.put("maxEpss", String.valueOf(request.maxEpss()));
    }
    if (request.hasMalware() != null) {
      params.put("hasMalware", String.valueOf(request.hasMalware()));
    }
    if (request.patchAvailable() != null) {
      params.put("patchAvailable", String.valueOf(request.patchAvailable()));
    }
    if (request.cwes() != null) {
      request.cwes().forEach(c -> params.put("cwes", c));
    }
    if (request.exploitationKnown() != null) {
      params.put("exploitationKnown", String.valueOf(request.exploitationKnown()));
    }
    if (request.publishedWindow() != null) {
      params.put("publishedWindow", request.publishedWindow());
    }
    return params;
  }

  @Authorize(permission = Permission.READ)
  @Override
  public ApiSearchResponse<ComponentDocument> getComponentDependencies(
      GuideComponentDependenciesRequest request)
  {
    try {
      return hdsClient.getWithMultimap(
          GuideComponentSearchResponse.class,
          "rest/search/components/dependencies",
          buildComponentDependenciesParams(request));
    }
    catch (NotFoundException e) {
      int limit = request.limit() != null ? request.limit() : 20;
      int offset = request.offset() != null ? request.offset() : 0;
      return new GuideComponentSearchResponse(List.of(), 0, offset, limit, null);
    }
    catch (BadGatewayException | InternalServerErrorException e) {
      throw new GuideApiException(Response.Status.BAD_GATEWAY,
          "Failed to retrieve component dependencies from data service");
    }
  }

  private static Multimap<String, String> buildComponentDependenciesParams(
      GuideComponentDependenciesRequest request)
  {
    Multimap<String, String> params = ArrayListMultimap.create();
    if (request.purl() != null) {
      params.put("purl", purlForUpstream(request.purl()));
    }
    if (request.format() != null) {
      params.put("format", request.format());
    }
    if (request.namespace() != null) {
      params.put("namespace", request.namespace());
    }
    if (request.name() != null) {
      params.put("name", request.name());
    }
    if (request.version() != null) {
      params.put("version", request.version());
    }
    if (request.query() != null) {
      params.put("query", request.query());
    }
    if (request.offset() != null) {
      params.put("offset", String.valueOf(request.offset()));
    }
    if (request.limit() != null) {
      params.put("limit", String.valueOf(request.limit()));
    }
    if (request.sortField() != null) {
      params.put("sortField", request.sortField());
    }
    if (request.sortOrder() != null) {
      params.put("sortOrder", request.sortOrder());
    }
    if (request.formats() != null) {
      request.formats().forEach(f -> params.put("formats", f));
    }
    if (request.categories() != null) {
      request.categories().forEach(c -> params.put("categories", c));
    }
    if (request.severities() != null) {
      request.severities().forEach(s -> params.put("severities", s));
    }
    if (request.minCvss() != null) {
      params.put("minCvss", String.valueOf(request.minCvss()));
    }
    if (request.maxCvss() != null) {
      params.put("maxCvss", String.valueOf(request.maxCvss()));
    }
    if (request.minVersionScore() != null) {
      params.put("minVersionScore", String.valueOf(request.minVersionScore()));
    }
    if (request.maxVersionScore() != null) {
      params.put("maxVersionScore", String.valueOf(request.maxVersionScore()));
    }
    if (request.licenseFamilies() != null) {
      request.licenseFamilies().forEach(l -> params.put("licenseFamilies", l));
    }
    if (request.licenses() != null) {
      request.licenses().forEach(l -> params.put("licenses", l));
    }
    if (request.latestStable() != null) {
      params.put("latestStable", request.latestStable());
    }
    return params;
  }

  @Authorize(permission = Permission.READ)
  @Override
  public ComponentDetailDocument getLatestVersionDetail(String purl) {
    try {
      return hdsClient.post(
          GuideComponentDetailDocument.class,
          "rest/search/components/latest-version",
          Map.of("purl", purlForUpstream(purl)));
    }
    catch (NotFoundException e) {
      throw notFound(e, "No latest version found for purl: " + purl);
    }
    catch (BadGatewayException | InternalServerErrorException e) {
      throw new GuideApiException(Response.Status.BAD_GATEWAY,
          "Failed to retrieve latest version from data service");
    }
  }

  /**
   * Builds a GuideNotFoundException carrying the most useful 404 message we can produce, with
   * the goal of matching Guide SaaS error responses as closely as possible without modifying
   * shared HDS-client infrastructure.
   *
   * <h3>Why this exists</h3>
   *
   * <p>
   * Guide SaaS (the seaworthy backend-server) and Guide self-hosted (this module) are
   * supposed to expose the same public API contract. For 404s, SaaS returns the upstream
   * search-server's detailed message verbatim — e.g. {@code
   * "Vulnerability not found: CVE-9999-99999"} or {@code "Component not found for PURL: <purl>
   * (format=..., namespace=..., name=..., version=...)"} — because Spring's
   * GlobalExceptionHandler propagates the NotFoundException's message into the response body.
   *
   * <p>
   * On IQ self-hosted, the call path is HdsClient → search-server. When the search-server
   * returns a JSON 404 body (which it does), {@code HdsClient.getErrorMessage} only reads the
   * body when {@code Content-Type: text/plain}; for JSON it falls through to {@code
   * statusLine.getReasonPhrase()} and ends up calling {@code new NotFoundException("Not
   * Found")}. The actual message in the response body is discarded before this method ever
   * sees the exception, so we cannot recover it here.
   *
   * <h3>What this helper does</h3>
   *
   * <p>
   * If the upstream message is non-blank AND not just the bare HTTP reason phrase, we
   * propagate it verbatim — the SaaS-equivalent path. This covers the case where HdsClient
   * does manage to read a useful message (e.g. a future text/plain HDS response) and the
   * future case where {@code HdsClient.getErrorMessage} is taught to parse JSON bodies.
   *
   * <p>
   * Otherwise we use the per-method {@code fallback} string. The fallback is hand-written
   * to be informative — typically {@code "<thing> not found: <key>"} — so customers see a
   * useful message instead of the unhelpful bare {@code "Not Found"}. The fallback won't
   * always match SaaS character-for-character (e.g. SaaS includes parsed PURL coordinates in
   * its component-detail message, which we don't currently reproduce here), but it preserves
   * the contract elements that matter: HTTP status, JSON body shape, and a non-empty
   * machine-readable {@code message} field.
   *
   * <h3>Why this isn't fixed in HdsClient</h3>
   *
   * <p>
   * The cleaner solution is for {@code HdsClient.getErrorMessage} to also parse {@code
   * application/json} bodies and extract the {@code message} field, which would benefit every
   * IQ caller (Lifecycle, Firewall, SBOM Manager, etc.) — not just Guide. That change has
   * wider blast radius and is tracked separately. This local fix gives Guide error-message
   * parity today without touching shared infrastructure.
   */
  private static GuideNotFoundException notFound(NotFoundException upstream, String fallback) {
    String upstreamMessage = upstream.getMessage();
    boolean useful = upstreamMessage != null
        && !upstreamMessage.isBlank()
        && !"Not Found".equalsIgnoreCase(upstreamMessage);
    String message = useful ? upstreamMessage : fallback;
    return new GuideNotFoundException(message);
  }

  /**
   * URL-encodes a path segment for safe inclusion in a URL path.
   * This prevents path traversal and injection attacks when user-supplied
   * values are concatenated into HDS URL paths.
   */
  private static String encodePathSegment(String segment) {
    if (segment == null) {
      return null;
    }
    return URLEncoder.encode(segment, StandardCharsets.UTF_8);
  }

  /**
   * Pre-encode {@code %} → {@code %25} on a PURL value before handing it to
   * {@link com.sonatype.insight.brain.hds.HdsClient}.
   *
   * <p>
   * Why: {@code HdsClient.buildUri} routes through {@link jakarta.ws.rs.core.UriBuilder}, whose
   * {@code queryParam(String, Object)} treats existing {@code %XX} sequences in the value as
   * already-encoded and leaves them alone. So a canonical PURL like
   * {@code pkg:npm/%40acceleratxr%2Fclient_sdk@1.14.0} reaches search-server (after one
   * URL-decode) as {@code pkg:npm/@acceleratxr/client_sdk@1.14.0} — which the search-server's
   * {@code PackageURL} parser then mis-reads as {@code (namespace=@acceleratxr, name=client_sdk)}
   * instead of {@code (namespace=null, name=@acceleratxr/client_sdk)}.
   *
   * <p>
   * Guide SaaS goes through Spring's {@code UriComponentsBuilder} which double-encodes the
   * {@code %} (template-mode encoding), so search-server sees the encoded-internals form and
   * parses it correctly. Pre-encoding {@code %} here makes the IQ→HDS wire format match the
   * SaaS→search-server wire format exactly. Without this, scoped npm packages 404 in
   * self-hosted Guide while working on SaaS.
   */
  private static String purlForUpstream(String purl) {
    if (purl == null) {
      return null;
    }
    return purl.replace("%", "%25");
  }
}
