/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.render;

import com.sonatype.insight.brain.git.render.model.MDImages;
import com.sonatype.insight.brain.utils.ThreatLevel;

import static com.sonatype.insight.brain.git.render.model.MDImages.CRITICAL_INDICATOR;
import static com.sonatype.insight.brain.git.render.model.MDImages.LOW_INDICATOR;
import static com.sonatype.insight.brain.git.render.model.MDImages.MODERATE_INDICATOR;
import static com.sonatype.insight.brain.git.render.model.MDImages.SEVERE_INDICATOR;
import static com.sonatype.insight.brain.git.render.model.MDImages.UNKNOWN_INDICATOR;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

public class ThreatLevelDisplay
{
  private final MDImages image;

  private final int value;

  public ThreatLevelDisplay(final MDImages image, final int value) {
    this.image = requireNonNull(image);
    this.value = value;
  }

  public MDImages getImage() {
    return image;
  }

  public int getValue() {
    return value;
  }

  private static MDImages resolveIndicatorImage(final ThreatLevel level) {
    if (!isNull(level)) {
      switch (level) {
        case CRITICAL:
          return CRITICAL_INDICATOR;
        case SEVERE:
          return SEVERE_INDICATOR;
        case MODERATE:
          return MODERATE_INDICATOR;
        case LOW:
          return LOW_INDICATOR;
        default:
          return UNKNOWN_INDICATOR;
      }
    }
    return UNKNOWN_INDICATOR;
  }

  public static ThreatLevelDisplay fromValue(final int value) {
    final ThreatLevel threatLevel = ThreatLevel.from(value);
    return new ThreatLevelDisplay(resolveIndicatorImage(threatLevel), value);
  }
}
