/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

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
