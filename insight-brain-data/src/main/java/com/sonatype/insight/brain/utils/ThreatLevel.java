/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

/**
 * @since 1.33
 */
public enum ThreatLevel
{
  LOW("Low"),
  MODERATE("Moderate"),
  SEVERE("Severe"),
  CRITICAL("Critical");

  ThreatLevel(final String displayName) {
    this.displayName = displayName;
  }

  private String displayName;

  public String getDisplayName() {
    return displayName;
  }

  public static ThreatLevel from(int threatLevel) {
    if (threatLevel >= 8) {
      return CRITICAL;
    }
    else if (threatLevel >= 4) {
      return SEVERE;
    }
    else if (threatLevel >= 2) {
      return MODERATE;
    }
    else {
      return LOW;
    }
  }
}
