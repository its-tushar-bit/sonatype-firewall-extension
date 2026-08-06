/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import java.util.Locale;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Severity;

/**
 * Validates Vulnerabilities list request fields at the API boundary.
 */
@Named
@Singleton
final class VulnerabilitiesListRequestValidator
{
  static final String TAB_MY_SCAN_DATA = "myScanData";

  static final String TAB_CATALOG = "catalog";

  static final String DEFAULT_ORDER_BY = "-cvssScore";

  static final float CVSS_MIN = 0.0f;

  static final float CVSS_MAX = 10.0f;

  static final float EPSS_MIN = 0.0f;

  static final float EPSS_MAX = 1.0f;

  private static final Set<String> SUPPORTED_ORDER_BY = Set.of("cvssScore", "-cvssScore");

  private static final Set<String> SUPPORTED_TABS = Set.of(TAB_MY_SCAN_DATA, TAB_CATALOG);

  static final Set<String> SUPPORTED_SEVERITIES =
      Set.of("critical", "high", "medium", "low", "none");

  static final Set<String> SUPPORTED_PUBLISHED_WINDOWS = Set.of("30d", "90d", "1y", "2y");

  private final Configuration configuration;

  @Inject
  VulnerabilitiesListRequestValidator(final Configuration configuration) {
    this.configuration = configuration;
  }

  void validate(final VulnerabilitiesListRequestDTO request) {
    if (request == null) {
      return;
    }
    validateTab(request.tab);
    validateOrderBy(request.orderBy);
    String tab = normalizeTab(request.tab);
    rejectUnsupportedFilters(request, tab);
    validateSeverities(request.severities);
    validateCvssRange(request.minCvssScore, request.maxCvssScore);
    validateEpssRange(request.minEpssScore, request.maxEpssScore);
    validateEcosystems(request.ecosystems);
    validateCwes(request.cwes);
    validatePublishedWindow(request.publishedWindow);
    validateScopeIds(request.organizationIds, "organizationIds");
    validateScopeIds(request.applicationIds, "applicationIds");
    validateScopeIds(request.stageIds, "stageIds");
  }

  /**
   * Scope filters are free-form ids resolved against the index, so the only boundary check is that
   * they are non-blank and within the clause budget. Oversized sets are rejected here rather than
   * silently truncated, since a truncated OR clause would quietly widen the result set.
   */
  private void validateScopeIds(final Set<String> ids, final String fieldName) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    for (String id : ids) {
      if (StringUtils.isBlank(id)) {
        throw new BadRequestException(fieldName + " must not contain blank values.");
      }
    }
    int maxClauseCount = configuration.getMaxAdvancedSearchClauseCount();
    if (maxClauseCount > 0 && ids.size() > maxClauseCount) {
      throw new BadRequestException(fieldName + " contains too many ids (max " + maxClauseCount + ").");
    }
  }

  static String normalizeTab(final String tab) {
    if (StringUtils.isBlank(tab)) {
      return TAB_MY_SCAN_DATA;
    }
    return tab.trim();
  }

  private static void validateTab(final String tab) {
    if (StringUtils.isBlank(tab)) {
      return;
    }
    if (!SUPPORTED_TABS.contains(tab.trim())) {
      throw new BadRequestException(
          "Invalid tab: " + tab + ". Supported values are myScanData and catalog.");
    }
  }

  private static void validateOrderBy(final String orderBy) {
    if (StringUtils.isBlank(orderBy)) {
      return;
    }
    if (!SUPPORTED_ORDER_BY.contains(orderBy)) {
      throw new BadRequestException(
          "Invalid orderBy: " + orderBy + ". Supported values are cvssScore and -cvssScore.");
    }
  }

  private static void rejectUnsupportedFilters(
      final VulnerabilitiesListRequestDTO request,
      final String tab)
  {
    boolean catalog = TAB_CATALOG.equals(tab);
    if (!catalog && request.knownExploited != null) {
      throw new BadRequestException(
          "knownExploited filter is only supported on the catalog tab.");
    }
    if (!catalog && request.malware != null) {
      throw new BadRequestException("malware filter is only supported on the catalog tab.");
    }
    if (!catalog && (request.minEpssScore != null || request.maxEpssScore != null)) {
      throw new BadRequestException("EPSS filters are only supported on the catalog tab.");
    }
    if (!catalog && request.cwes != null && !request.cwes.isEmpty()) {
      throw new BadRequestException("cwes filter is only supported on the catalog tab.");
    }
    if (!catalog && StringUtils.isNotBlank(request.publishedWindow)) {
      throw new BadRequestException("publishedWindow filter is only supported on the catalog tab.");
    }
    if (request.patchAvailable != null) {
      throw new BadRequestException("patchAvailable filter is not yet supported on the vulnerabilities list.");
    }
    if (request.policyCompliance != null && !request.policyCompliance.isEmpty()) {
      throw new BadRequestException(
          "policyCompliance filter is not yet supported on the vulnerabilities list.");
    }
  }

  private static void validateSeverities(final Set<String> severities) {
    if (severities == null || severities.isEmpty()) {
      return;
    }
    for (String severity : severities) {
      if (StringUtils.isBlank(severity) || !SUPPORTED_SEVERITIES.contains(severity.trim().toLowerCase(Locale.ROOT))) {
        throw new BadRequestException(
            "Invalid severity: " + severity
                + ". Supported values are critical, high, medium, low, and none.");
      }
    }
  }

  private static void validateCvssRange(final Float minCvssScore, final Float maxCvssScore) {
    if (minCvssScore != null) {
      requireInCvssDomain(minCvssScore, "minCvssScore");
    }
    if (maxCvssScore != null) {
      requireInCvssDomain(maxCvssScore, "maxCvssScore");
    }
    if (minCvssScore != null && maxCvssScore != null && minCvssScore > maxCvssScore) {
      throw new BadRequestException(
          "Invalid CVSS range: minCvssScore (" + minCvssScore + ") is greater than maxCvssScore ("
              + maxCvssScore + ").");
    }
  }

  private static void validateEpssRange(final Float minEpssScore, final Float maxEpssScore) {
    if (minEpssScore != null) {
      requireInEpssDomain(minEpssScore, "minEpssScore");
    }
    if (maxEpssScore != null) {
      requireInEpssDomain(maxEpssScore, "maxEpssScore");
    }
    if (minEpssScore != null && maxEpssScore != null && minEpssScore > maxEpssScore) {
      throw new BadRequestException(
          "Invalid EPSS range: minEpssScore (" + minEpssScore + ") is greater than maxEpssScore ("
              + maxEpssScore + ").");
    }
  }

  private static void requireInCvssDomain(final float score, final String fieldName) {
    if (score < CVSS_MIN || score > CVSS_MAX) {
      throw new BadRequestException(
          "Invalid " + fieldName + ": " + score + ". Value must be between " + CVSS_MIN + " and "
              + CVSS_MAX + ".");
    }
  }

  private static void requireInEpssDomain(final float score, final String fieldName) {
    if (score < EPSS_MIN || score > EPSS_MAX) {
      throw new BadRequestException(
          "Invalid " + fieldName + ": " + score + ". Value must be between " + EPSS_MIN + " and "
              + EPSS_MAX + ".");
    }
  }

  private static void validateEcosystems(final Set<String> ecosystems) {
    if (ecosystems == null || ecosystems.isEmpty()) {
      return;
    }
    for (String ecosystem : ecosystems) {
      if (StringUtils.isBlank(ecosystem)) {
        throw new BadRequestException("ecosystems must not contain blank values.");
      }
    }
  }

  /**
   * CWE ids are opaque facet keys from HDS (not necessarily {@code CWE-\d+}). Reject blanks only;
   * non-blank values are forwarded as-is and URL-encoded by the HDS client. Cap set size with the
   * same advanced-search clause budget used for scope ids so IQ fails with a clear 400 instead of
   * forwarding an unbounded {@code cwes=} fan-out to HDS.
   */
  private void validateCwes(final Set<String> cwes) {
    if (cwes == null || cwes.isEmpty()) {
      return;
    }
    for (String cwe : cwes) {
      if (StringUtils.isBlank(cwe)) {
        throw new BadRequestException("cwes must not contain blank values.");
      }
    }
    int maxClauseCount = configuration.getMaxAdvancedSearchClauseCount();
    if (maxClauseCount > 0 && cwes.size() > maxClauseCount) {
      throw new BadRequestException("cwes contains too many ids (max " + maxClauseCount + ").");
    }
  }

  private static void validatePublishedWindow(final String publishedWindow) {
    if (StringUtils.isBlank(publishedWindow)) {
      return;
    }
    String normalized = publishedWindow.trim().toLowerCase(Locale.ROOT);
    if (!SUPPORTED_PUBLISHED_WINDOWS.contains(normalized)) {
      throw new BadRequestException(
          "Invalid publishedWindow: " + publishedWindow
              + ". Supported values are 30d, 90d, 1y, and 2y.");
    }
  }

  /**
   * Maps a CVSS score to a V1 severity facet/card band ({@code critical}|{@code high}|{@code medium}|
   * {@code low}|{@code none}). Scores outside every CVSS v3 band (gaps / out-of-range) map to
   * {@code none} so the FE never receives an unexpected {@code unknown} token.
   */
  static String severityBand(final Float cvssScore) {
    if (cvssScore == null) {
      return null;
    }
    Severity rating = CvssV3Severity.resolveRatingSeverity(cvssScore);
    if (rating == null || rating == Severity.UNKNOWN) {
      return CvssV3Severity.NONE.name().toLowerCase(Locale.ROOT);
    }
    return rating.name().toLowerCase(Locale.ROOT);
  }

  static CvssV3Severity toCvssV3Severity(final String severityToken) {
    return CvssV3Severity.valueOf(severityToken.trim().toUpperCase(Locale.ROOT));
  }
}
