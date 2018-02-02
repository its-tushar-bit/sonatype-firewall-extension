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
  private long timestamp;

  private Long duration;

  private Map<String, String> attributes = new HashMap<>();

  /**
   * Unused constructor required for JSON deserialization support.
   */
  @SuppressWarnings("unused")
  private TelemetryData() {
  }

  public TelemetryData(final long timestamp) {
    this.timestamp = timestamp;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public Long getDuration() {
    return duration;
  }

  public void setDuration(final Long duration) {
    this.duration = duration;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(final Map<String, String> attributes) {
    this.attributes = attributes;
  }
}
