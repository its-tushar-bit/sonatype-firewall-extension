/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

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

  private Map<String, Object> attributes = new HashMap<>();

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

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public void setAttributes(final Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  public TelemetryPurpose getPurpose() {
    return purpose;
  }

  public void setPurpose(TelemetryPurpose purpose) {
    this.purpose = purpose;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || other.getClass() != getClass()) {
      return false;
    }

    TelemetryData otherData = (TelemetryData) other;

    return new EqualsBuilder().append(purpose, otherData.purpose).append(timestamp, otherData.timestamp)
        .append(attributes, otherData.attributes).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder().append(purpose).append(timestamp).append(attributes).toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this).append("purpose", purpose).append("timestamp", timestamp)
        .append("attributes", attributes).toString();
  }
}
