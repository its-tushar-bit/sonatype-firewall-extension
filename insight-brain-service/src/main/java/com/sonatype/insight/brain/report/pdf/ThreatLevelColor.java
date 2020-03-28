/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.awt.*;

public final class ThreatLevelColor
{
  // Visible for testing
  static final Color CRITICAL_THREAT_COLOR = Color.decode("#bc012f");

  // Visible for testing
  static final Color SEVERE_THREAT_COLOR = Color.decode("#f4861d");

  // Visible for testing
  static final Color MODERATE_THREAT_COLOR = Color.decode("#f5c648");

  // Visible for testing
  static final Color LOW_THREAT_COLOR = Color.decode("#006bbf");

  // Visible for testing
  static final Color NO_THREAT_COLOR = Color.decode("#97cbee");

  private ThreatLevelColor() {
    throw new UnsupportedOperationException();
  }

  public static Color get(int threatLevel) {
    switch (threatLevel) {
      case 10:
      case 9:
      case 8:
        return CRITICAL_THREAT_COLOR;
      case 7:
      case 6:
      case 5:
      case 4:
        return SEVERE_THREAT_COLOR;
      case 3:
      case 2:
        return MODERATE_THREAT_COLOR;
      case 1:
        return LOW_THREAT_COLOR;
      case 0:
        return NO_THREAT_COLOR;
      default:
        throw new IllegalStateException("Unknown threat level " + threatLevel + ".");
    }
  }
}
