/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.core;

import com.sonatype.guide.api.dto.AffectedComponentVersion;
import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.guide.api.dto.ComponentDetailDocument;
import com.sonatype.guide.api.dto.ComponentDocument;
import com.sonatype.guide.api.dto.SearchResult;
import com.sonatype.guide.api.dto.VulnerabilityDetailDocument;
import com.sonatype.guide.api.dto.VulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersionRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDependenciesRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentSearchRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentVersionsRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentVulnerabilitiesRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideGlobalSearchRequest;
import com.sonatype.insight.brain.guide.api.dto.GuideRecommendationResult;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchRequest;

/**
 * Search-server client used by both the Guide REST API and the Guide MCP tools.
 *
 * <p>
 * The interface deliberately exposes two flavors of the same lookups because the two callers
 * have different needs:
 *
 * <ul>
 * <li><b>Raw-string methods</b> ({@link #getComponentByPurl}, {@link #getLatestComponentVersion})
 * are used exclusively by the Guide MCP tools. MCP tool responses are stitched together
 * (per-purl batching, error envelopes, policy annotation) by
 * {@code McpResponseFormatter}, which parses the raw JSON itself, so the client returns the
 * upstream JSON unparsed.</li>
 * <li><b>Typed methods</b> ({@link #getComponentDetailByPurl}, {@link #getLatestVersionDetail},
 * {@link #searchComponents}, etc.) are used by the JAX-RS resources. They return parsed DTOs
 * matching the Guide SaaS API contract so the resource layer can pass them directly to
 * Jackson.</li>
 * </ul>
 *
 * <p>
 * If the MCP tools are ever rewritten to consume typed DTOs, the raw-string methods can be
 * deleted. Until then, prefer the typed variant when adding new callers.
 */
public interface SearchApiClient
{
  /**
   * Get component detail by PURL. Returns JSON string from search-server.
   *
   * <p>
   * <b>MCP only.</b> The Guide REST API uses {@link #getComponentDetailByPurl} instead.
   */
  String getComponentByPurl(String purl);

  /**
   * Get latest version of a component by PURL.
   *
   * <p>
   * <b>MCP only.</b> The Guide REST API uses {@link #getLatestVersionDetail} instead.
   */
  String getLatestComponentVersion(String purl);

  /**
   * Get upgrade recommendations for a specific artifact of a component.
   *
   * <p>
   * The {@code extension} and {@code classifier} parameters identify which artifact of the
   * component to target (e.g., Maven {@code sources} or {@code javadoc} artifacts). When both are
   * {@code null} or blank, the existing component-level recommendation path runs unchanged.
   *
   * <p>
   * Blank values (empty string or whitespace-only) are treated identically to {@code null} — the
   * upstream request omits the field rather than sending empty strings.
   *
   * @param purl the component PURL (required)
   * @param extension the artifact extension/file type (e.g., "war", "jar", "pom"); blank or null
   *          for component-level recommendations
   * @param classifier the artifact classifier (e.g., "sources", "javadoc"); blank or null for
   *          component-level recommendations
   * @return the recommendation result; never {@code null}
   * @throws com.sonatype.insight.brain.guide.api.error.GuideNotFoundException if no
   *           recommendations exist for the given PURL. Implementations MUST throw rather than
   *           return {@code null} — Guide resource methods forward the result directly to JAX-RS,
   *           which would serialize {@code null} as a 200 OK with an empty body and silently break
   *           the 404 contract.
   */
  GuideRecommendationResult getRecommendations(String purl, String extension, String classifier);

  /**
   * Search components with the given filters.
   */
  ApiSearchResponse<ComponentDocument> searchComponents(GuideComponentSearchRequest request);

  /**
   * Search vulnerabilities with the given filters.
   */
  ApiSearchResponse<VulnerabilityDocument> searchVulnerabilities(GuideVulnerabilitySearchRequest request);

  /**
   * Global search across components and vulnerabilities.
   */
  ApiSearchResponse<SearchResult> globalSearch(GuideGlobalSearchRequest request);

  /**
   * Get vulnerability detail by ref ID.
   *
   * @return the vulnerability detail; never {@code null}
   * @throws com.sonatype.insight.brain.guide.api.error.GuideNotFoundException if no
   *           vulnerability matches the given ref ID. Implementations MUST throw rather than
   *           return {@code null}; see {@link #getRecommendations} for rationale.
   */
  VulnerabilityDetailDocument getVulnerabilityByRefId(String id);

  /**
   * Get affected components for a vulnerability.
   */
  ApiSearchResponse<AffectedComponentVersion> getVulnerabilityAffectedComponents(
      GuideAffectedComponentVersionRequest request);

  /**
   * Get component detail by PURL (typed response).
   *
   * @return the component detail; never {@code null}
   * @throws com.sonatype.insight.brain.guide.api.error.GuideNotFoundException if no
   *           component matches the given PURL. Implementations MUST throw rather than
   *           return {@code null}; see {@link #getRecommendations} for rationale.
   */
  ComponentDetailDocument getComponentDetailByPurl(String purl);

  /**
   * Get component versions list.
   */
  ApiSearchResponse<ComponentDetailDocument> getComponentVersions(GuideComponentVersionsRequest request);

  /**
   * Get component vulnerabilities.
   */
  ApiSearchResponse<VulnerabilityDocument> getComponentVulnerabilities(
      GuideComponentVulnerabilitiesRequest request);

  /**
   * Get component dependencies.
   */
  ApiSearchResponse<ComponentDocument> getComponentDependencies(GuideComponentDependenciesRequest request);

  /**
   * Get latest version detail by PURL (typed response).
   *
   * @return the latest-version detail; never {@code null}
   * @throws com.sonatype.insight.brain.guide.api.error.GuideNotFoundException if no
   *           latest version is available for the given PURL. Implementations MUST throw rather
   *           than return {@code null}; see {@link #getRecommendations} for rationale.
   */
  ComponentDetailDocument getLatestVersionDetail(String purl);
}
