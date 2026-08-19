/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.cyclonedx.model.vulnerability.Vulnerability.Rating.Severity;

public enum CvssV3Severity
{
  NONE(0f, 0f, "None"),
  LOW(0.1f, 3.9f, "Low"),
  MEDIUM(4.0f, 6.9f, "Medium"),
  HIGH(7.0f, 8.9f, "High"),
  CRITICAL(9.0f, 10.0f, "Critical");

  // Inclusive
  private final float startScoreRange;

  // Inclusive
  private final float endScoreRange;

  private final String displayName;

  CvssV3Severity(float startScoreRange, float endScoreRange, String displayName) {
    this.startScoreRange = startScoreRange;
    this.endScoreRange = endScoreRange;
    this.displayName = displayName;
  }

  public float getStartScoreRange() {
    return startScoreRange;
  }

  public float getEndScoreRange() {
    return endScoreRange;
  }

  public String getDisplayName() {
    return displayName;
  }

  /**
   * Half-open {@code [minInclusive, maxExclusive)} CVSS-score bands keyed by lowercase display name
   * ({@code none}, {@code low}, {@code medium}, {@code high}, {@code critical}), in ascending order.
   * These are the boundaries the search float-range aggregation primitive
   * ({@code SearchIndexClient.aggregateCountByFloatField}) and the catalog severity facet both consume,
   * so a single source of truth defines where each boundary value lands.
   * <p>
   * The declared inclusive display ranges (e.g. {@code LOW = 0.1..3.9}, {@code HIGH = 7.0..8.9}) leave
   * gaps between the printed upper bound and the next band's lower bound. CVSS v3 base scores are always
   * quantized to one decimal place so no real score falls in a gap, but a half-open convention makes the
   * bands mathematically contiguous and unambiguous for any float: the exclusive upper bound of one band
   * equals the inclusive lower bound of the next, so a boundary score belongs to exactly one band —
   * {@code 4.0} is Medium (not Low), {@code 7.0} is High (not Medium), {@code 9.0} is Critical (not High).
   * {@code NONE} is exactly {@code 0.0}: {@code [0.0, Math.nextUp(0.0))}. {@code CRITICAL}'s upper bound is
   * {@code Math.nextUp(10.0)} so a maximum score of {@code 10.0} is included.
   */
  private static final Map<String, float[]> HALF_OPEN_SCORE_BANDS;

  static {
    Map<String, float[]> bands = new LinkedHashMap<>();
    bands.put(NONE.name().toLowerCase(Locale.ROOT),
        new float[]{NONE.startScoreRange, Math.nextUp(NONE.startScoreRange)});
    bands.put(LOW.name().toLowerCase(Locale.ROOT), new float[]{LOW.startScoreRange, MEDIUM.startScoreRange});
    bands.put(MEDIUM.name().toLowerCase(Locale.ROOT), new float[]{MEDIUM.startScoreRange, HIGH.startScoreRange});
    bands.put(HIGH.name().toLowerCase(Locale.ROOT), new float[]{HIGH.startScoreRange, CRITICAL.startScoreRange});
    bands.put(
        CRITICAL.name().toLowerCase(Locale.ROOT),
        new float[]{CRITICAL.startScoreRange, Math.nextUp(CRITICAL.endScoreRange)});
    HALF_OPEN_SCORE_BANDS = Collections.unmodifiableMap(bands);
  }

  public static Map<String, float[]> halfOpenScoreBands() {
    return HALF_OPEN_SCORE_BANDS;
  }

  /**
   * Subset of {@link #halfOpenScoreBands()} for the given severities, keyed by lowercase enum name,
   * in enum declaration order.
   */
  public static Map<String, float[]> halfOpenScoreBands(Set<CvssV3Severity> severities) {
    if (severities == null || severities.isEmpty()) {
      return Map.of();
    }
    Map<String, float[]> selected = new LinkedHashMap<>();
    for (CvssV3Severity severity : values()) {
      if (severities.contains(severity)) {
        String key = severity.name().toLowerCase(Locale.ROOT);
        selected.put(key, HALF_OPEN_SCORE_BANDS.get(key));
      }
    }
    return Collections.unmodifiableMap(selected);
  }

  public static Severity resolveRatingSeverity(float severityScore) {
    Severity ratingSeverity = null;

    if (severityScore == NONE.getStartScoreRange()) {
      ratingSeverity = Severity.NONE;
    }
    else if (severityScore >= LOW.getStartScoreRange() && severityScore <= LOW.getEndScoreRange()) {
      ratingSeverity = Severity.LOW;
    }
    else if (severityScore >= MEDIUM.getStartScoreRange() && severityScore <= MEDIUM.getEndScoreRange()) {
      ratingSeverity = Severity.MEDIUM;
    }
    else if (severityScore >= HIGH.getStartScoreRange() && severityScore <= HIGH.getEndScoreRange()) {
      ratingSeverity = Severity.HIGH;
    }
    else if (severityScore >= CRITICAL.getStartScoreRange() && severityScore <= CRITICAL.getEndScoreRange()) {
      ratingSeverity = Severity.CRITICAL;
    }
    else {
      ratingSeverity = Severity.UNKNOWN;
    }
    return ratingSeverity;
  }
}
