/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 1.43.0
 */
public class TelemetryData
{
  /**
   * @since 1.46
   */
  private TelemetryPurpose purpose;

  private long timestamp;

  private Map<String, String> attributes = new HashMap<>();

  /**
   * Unused constructor required for JSON deserialization support.
   */
  @SuppressWarnings("unused")
  private TelemetryData() {
  }

  public TelemetryData(final TelemetryPurpose purpose, final long timestamp) {
    this.purpose = purpose;
    this.timestamp = timestamp;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(final Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public TelemetryPurpose getPurpose() {
    return purpose;
  }

  public void setPurpose(TelemetryPurpose purpose) {
    this.purpose = purpose;
  }
}
