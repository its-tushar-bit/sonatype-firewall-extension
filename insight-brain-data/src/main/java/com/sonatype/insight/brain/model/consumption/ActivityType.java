/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.consumption;

/**
 * Activity types for consumption-based pricing.
 * <p>
 * {@code displayBucket} is intentionally non-unique: write-side is granular (APP_SCAN vs
 * RE_EVALUATE), read-side groups them. Don't reverse-map from displayBucket.
 *
 * @since 1.204
 */
public enum ActivityType
{
  APP_SCAN("App Scan + Re-evaluate"),
  RE_EVALUATE("App Scan + Re-evaluate"),
  CONTINUOUS_MONITORING("Continuous Monitoring"),
  COMPONENT_DETAILS("Component Details"),
  VERSION_RECOMMENDATION("Version Recommendations"),
  REACHABILITY("Reachability Analysis"),
  API("APIs"),
  DEVELOPER_PRIORITIES("Version Recommendations"),
  OTHERS("Others");

  private final String displayBucket;

  ActivityType(String displayBucket) {
    this.displayBucket = displayBucket;
  }

  public String getDisplayBucket() {
    return displayBucket;
  }
}
