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

  private static final Set<String> SUPPORTED_ORDER_BY = Set.of("cvssScore", "-cvssScore");

  private static final Set<String> SUPPORTED_TABS = Set.of(TAB_MY_SCAN_DATA, TAB_CATALOG);

  static final Set<String> SUPPORTED_SEVERITIES =
      Set.of("critical", "high", "medium", "low", "none");

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
    rejectUnsupportedFilters(request);
    validateSeverities(request.severities);
    validateCvssRange(request.minCvssScore, request.maxCvssScore);
    validateEcosystems(request.ecosystems);
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

  private static void rejectUnsupportedFilters(final VulnerabilitiesListRequestDTO request) {
    if (request.knownExploited != null) {
      throw new BadRequestException("knownExploited filter is not yet supported on the vulnerabilities list.");
    }
    if (request.malware != null) {
      throw new BadRequestException("malware filter is not yet supported on the vulnerabilities list.");
    }
    if (request.patchAvailable != null) {
      throw new BadRequestException("patchAvailable filter is not yet supported on the vulnerabilities list.");
    }
    if (request.cwes != null && !request.cwes.isEmpty()) {
      throw new BadRequestException("cwes filter is not yet supported on the vulnerabilities list.");
    }
    if (StringUtils.isNotBlank(request.publishedWindow)) {
      throw new BadRequestException(
          "publishedWindow filter is not yet supported on the vulnerabilities list.");
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

  private static void requireInCvssDomain(final float score, final String fieldName) {
    if (score < CVSS_MIN || score > CVSS_MAX) {
      throw new BadRequestException(
          "Invalid " + fieldName + ": " + score + ". Value must be between " + CVSS_MIN + " and "
              + CVSS_MAX + ".");
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
