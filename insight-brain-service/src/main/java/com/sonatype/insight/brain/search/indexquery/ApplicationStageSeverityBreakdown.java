/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses the multi-valued {@code applicationStageSeverityCount} tokens
 * ({@code "stage:severity:count"}, e.g. {@code "build:critical:3"}) into the per-stage severity
 * breakdown the application evaluation-card pills render.
 * <p>
 * Each returned stage carries the four {@code ThreatLevel} buckets (low/moderate/severe/critical);
 * absent buckets default to zero. A per-application total-risk rollup sums every stage. Malformed
 * tokens (wrong delimiter count, non-numeric count) are skipped rather than failing the row.
 */
final class ApplicationStageSeverityBreakdown
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationStageSeverityBreakdown.class);

  static final String CRITICAL = "critical";

  static final String SEVERE = "severe";

  static final String MODERATE = "moderate";

  static final String LOW = "low";

  private static final List<String> SEVERITIES = List.of(LOW, MODERATE, SEVERE, CRITICAL);

  /** Per-stage severity counts plus a whole-application {@code totalRisk} rollup. */
  record Breakdown(Map<String, Map<String, Integer>> stages, Map<String, Integer> totalRisk)
  {
  }

  private ApplicationStageSeverityBreakdown() {
  }

  /**
   * Parse the raw tokens. Returns {@code null} when there is nothing to surface (null/empty input or
   * every token malformed), so the row omits the breakdown rather than emitting an empty object.
   */
  static Breakdown parse(final Collection<String> tokens) {
    if (tokens == null || tokens.isEmpty()) {
      return null;
    }
    final Map<String, Map<String, Integer>> stages = new LinkedHashMap<>();
    final Map<String, Integer> totalRisk = zeroBuckets();
    boolean any = false;
    for (String token : tokens) {
      if (token == null) {
        continue;
      }
      // Split on the two structural colons only; a stage/severity value never contains one.
      final String[] parts = token.split(":", 3);
      if (parts.length != 3) {
        log.warn("Skipping malformed applicationStageSeverityCount token (expected stage:severity:count): {}", token);
        continue;
      }
      final String stage = parts[0].strip();
      final String severity = parts[1].strip().toLowerCase(Locale.ROOT);
      if (stage.isEmpty() || !SEVERITIES.contains(severity)) {
        log.warn("Skipping applicationStageSeverityCount token with unknown stage/severity: {}", token);
        continue;
      }
      final int count;
      try {
        count = Integer.parseInt(parts[2].strip());
      }
      catch (NumberFormatException e) {
        log.warn("Skipping applicationStageSeverityCount token with non-numeric count: {}", token);
        continue;
      }
      if (count < 0) {
        continue;
      }
      final Map<String, Integer> bucket = stages.computeIfAbsent(stage, k -> zeroBuckets());
      bucket.merge(severity, count, Integer::sum);
      totalRisk.merge(severity, count, Integer::sum);
      any = true;
    }
    return any ? new Breakdown(stages, totalRisk) : null;
  }

  private static Map<String, Integer> zeroBuckets() {
    final Map<String, Integer> bucket = new LinkedHashMap<>();
    for (String severity : SEVERITIES) {
      bucket.put(severity, 0);
    }
    return bucket;
  }
}
