/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Response;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sonatype.guide.api.dto.ApiSearchResponse;
import com.sonatype.guide.api.dto.VulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilitySearchResponse;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import com.sonatype.insight.brain.guide.api.error.GuideLicenseUnavailableException;
import com.sonatype.insight.brain.guide.core.GuideLicenseRevocationHandler;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.error.exception.BadGatewayException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.error.exception.PaymentRequiredException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sonatype Catalog path for Martha Vulnerabilities list (CLM-42216).
 * <p>
 * Calls HDS {@code rest/search/vulnerabilities} with the same query shape as Guide search, without
 * Guide {@code SearchApiClient}'s root-org {@code READ} gate — Catalog is available to the same
 * authenticated Preview audience as My Scan Data. Lazy on the FE via tab switch.
 */
@Named
@Singleton
final class VulnerabilitiesCatalogListService
{
  private static final Logger log = LoggerFactory.getLogger(VulnerabilitiesCatalogListService.class);

  private static final String HDS_VULNERABILITY_SEARCH = "rest/search/vulnerabilities";

  private final HdsClient hdsClient;

  private final GuideLicenseRevocationHandler revocationHandler;

  @Inject
  VulnerabilitiesCatalogListService(
      final HdsClient hdsClient,
      final GuideLicenseRevocationHandler revocationHandler)
  {
    this.hdsClient = hdsClient;
    this.revocationHandler = revocationHandler;
  }

  VulnerabilitiesListResponseDTO listCatalog(
      final VulnerabilitiesListRequestDTO request,
      final int page,
      final int pageSize,
      final boolean includeFacets)
  {
    String orderBy = request == null || StringUtils.isBlank(request.orderBy)
        ? VulnerabilitiesListRequestValidator.DEFAULT_ORDER_BY
        : request.orderBy;
    boolean ascending = "cvssScore".equals(orderBy);

    long offset = pageOffset(page, pageSize);
    Multimap<String, String> params = buildParams(request, pageSize, ascending, offset);
    ApiSearchResponse<VulnerabilityDocument> searchResult = searchHds(params, pageSize, offset);

    List<VulnerabilityRowDTO> rows = new ArrayList<>();
    if (searchResult != null && searchResult.hits() != null) {
      for (VulnerabilityDocument doc : searchResult.hits()) {
        rows.add(toRow(doc));
      }
    }

    long total = searchResult == null ? 0L : searchResult.total();
    VulnerabilitiesListResponseDTO response = new VulnerabilitiesListResponseDTO();
    response.vulnerabilities = rows;
    response.total = total;
    response.page = page;
    response.pageSize = pageSize;
    response.hasNextPage = hasNextPage(page, pageSize, total);
    response.source = VulnerabilitiesListResponseDTO.SOURCE_CATALOG;
    if (includeFacets) {
      response.facets = toFacets(searchResult, total);
    }
    return response;
  }

  private ApiSearchResponse<VulnerabilityDocument> searchHds(
      final Multimap<String, String> params,
      final int pageSize,
      final long offset)
  {
    try {
      return withLicenseRefreshOn402(
          () -> hdsClient.getWithMultimap(GuideVulnerabilitySearchResponse.class, HDS_VULNERABILITY_SEARCH, params));
    }
    catch (NotFoundException e) {
      int safeOffset = offset > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) offset;
      return new GuideVulnerabilitySearchResponse(List.of(), 0, safeOffset, pageSize, null);
    }
    catch (BadGatewayException | InternalServerErrorException e) {
      throw new GuideApiException(Response.Status.BAD_GATEWAY,
          "Failed to retrieve vulnerability search results from data service");
    }
  }

  private <T> T withLicenseRefreshOn402(final Supplier<T> hdsCall) {
    try {
      return hdsCall.get();
    }
    catch (PaymentRequiredException e) {
      try {
        revocationHandler.onPaymentRequired(HDS_VULNERABILITY_SEARCH);
      }
      catch (RuntimeException refreshFailure) {
        log.warn("Guide license refresh failed for endpoint {}; returning deterministic 402 anyway",
            HDS_VULNERABILITY_SEARCH, refreshFailure);
      }
      throw new GuideLicenseUnavailableException(Response.Status.PAYMENT_REQUIRED,
          "Guide feature is no longer licensed");
    }
  }

  private static Multimap<String, String> buildParams(
      final VulnerabilitiesListRequestDTO request,
      final int pageSize,
      final boolean ascending,
      final long offset)
  {
    Multimap<String, String> params = ArrayListMultimap.create();
    String search = blankToNull(request == null ? null : request.search);
    if (search != null) {
      params.put("query", search);
    }
    params.put("offset", String.valueOf(offset));
    params.put("limit", String.valueOf(pageSize));
    params.put("sortField", "cvssSeverity");
    params.put("sortOrder", ascending ? "asc" : "desc");
    putAll(params, "severities", request == null ? null : request.severities);
    if (request != null && request.minCvssScore != null) {
      // String.valueOf(Float) preserves the decimal (e.g. 7.1); Float.doubleValue() widens and can
      // send 7.099999904632568 to HDS, excluding hits scored exactly 7.1.
      params.put("minCvss", String.valueOf(request.minCvssScore));
    }
    if (request != null && request.maxCvssScore != null) {
      params.put("maxCvss", String.valueOf(request.maxCvssScore));
    }
    if (request != null && request.minEpssScore != null) {
      params.put("minEpss", String.valueOf(request.minEpssScore));
    }
    if (request != null && request.maxEpssScore != null) {
      params.put("maxEpss", String.valueOf(request.maxEpssScore));
    }
    if (request != null && request.knownExploited != null) {
      params.put("exploitationKnown", String.valueOf(request.knownExploited));
    }
    if (request != null && request.malware != null) {
      params.put("hasMalware", String.valueOf(request.malware));
    }
    putAllPreserveCase(params, "cwes", request == null ? null : request.cwes);
    if (request != null && StringUtils.isNotBlank(request.publishedWindow)) {
      params.put("publishedWindow", request.publishedWindow.trim().toLowerCase(Locale.ROOT));
    }
    putAll(params, "affectedEcosystems", request == null ? null : request.ecosystems);
    return params;
  }

  /** Offset sent to HDS — widen before multiply so large {@code page} cannot wrap to negative. */
  static long pageOffset(final int page, final int pageSize) {
    return (long) page * pageSize;
  }

  /** Widen before {@code page + 1} so {@code Integer.MAX_VALUE} cannot wrap the addition. */
  static boolean hasNextPage(final int page, final int pageSize, final long total) {
    return ((long) page + 1) * pageSize < total;
  }

  private static void putAll(final Multimap<String, String> params, final String key, final Set<String> values) {
    if (values == null || values.isEmpty()) {
      return;
    }
    for (String value : values) {
      if (StringUtils.isNotBlank(value)) {
        params.put(key, value.trim().toLowerCase(Locale.ROOT));
      }
    }
  }

  /** CWE ids keep original casing (e.g. {@code CWE-79}); only trim blanks. */
  private static void putAllPreserveCase(
      final Multimap<String, String> params,
      final String key,
      final Set<String> values)
  {
    if (values == null || values.isEmpty()) {
      return;
    }
    for (String value : values) {
      if (StringUtils.isNotBlank(value)) {
        params.put(key, value.trim());
      }
    }
  }

  private static VulnerabilitiesListFacetsDTO toFacets(
      final ApiSearchResponse<VulnerabilityDocument> searchResult,
      final long total)
  {
    VulnerabilitiesListFacetsDTO facets = new VulnerabilitiesListFacetsDTO();
    facets.totalVulnerabilities = total;
    if (searchResult == null || searchResult.aggregations() == null) {
      return facets;
    }
    Map<String, Map<String, Long>> aggregations = searchResult.aggregations();
    facets.severities = copyAggregation(aggregations, "severities", "severity", true);
    facets.ecosystems = copyAggregation(aggregations, "affectedEcosystems", "ecosystems", true);
    Map<String, Long> cwes = copyAggregation(aggregations, "cwes", "cwe", false);
    if (!cwes.isEmpty()) {
      facets.cwes = cwes;
    }
    return facets;
  }

  private static Map<String, Long> copyAggregation(
      final Map<String, Map<String, Long>> aggregations,
      final String primaryKey,
      final String alternateKey,
      final boolean lowerCaseKeys)
  {
    Map<String, Long> source = aggregations.get(primaryKey);
    if (source == null) {
      source = aggregations.get(alternateKey);
    }
    if (source == null || source.isEmpty()) {
      return new LinkedHashMap<>();
    }
    Map<String, Long> copy = new LinkedHashMap<>();
    source.forEach((key, value) -> {
      if (key != null && value != null) {
        copy.put(lowerCaseKeys ? key.toLowerCase(Locale.ROOT) : key, value);
      }
    });
    return copy;
  }

  private static VulnerabilityRowDTO toRow(final VulnerabilityDocument doc) {
    VulnerabilityRowDTO row = new VulnerabilityRowDTO();
    row.vulnerabilityId = doc.refid();
    row.title = doc.summary();
    Double score = doc.sonatypeCvssSeverity() != null ? doc.sonatypeCvssSeverity() : doc.cvssSeverity();
    if (score != null) {
      row.cvssScore = score.floatValue();
      row.severity = VulnerabilitiesListRequestValidator.severityBand(row.cvssScore);
    }
    if (doc.affectedEcosystems() != null && !doc.affectedEcosystems().isEmpty()) {
      row.ecosystem = doc.affectedEcosystems().get(0);
    }
    // Catalog chrome is on GuideVulnerabilityDocument (HDS hit). Cast keeps us off the thin
    // VulnerabilityDocument interface surface and avoids per-row detail fetches.
    if (doc instanceof GuideVulnerabilityDocument guide) {
      if (guide.kev() != null) {
        row.knownExploited = guide.kev();
      }
      if (guide.isMalware() != null) {
        row.malware = guide.isMalware();
      }
      if (guide.epss() != null) {
        row.epssScore = guide.epss().floatValue();
      }
      if (guide.publishedAt() != null) {
        row.publishedAt = guide.publishedAt().toString();
      }
      if (guide.cwes() != null && !guide.cwes().isEmpty()) {
        row.cwes = List.copyOf(guide.cwes());
      }
    }
    return row;
  }

  private static String blankToNull(final String value) {
    return StringUtils.isBlank(value) ? null : value.trim();
  }
}
