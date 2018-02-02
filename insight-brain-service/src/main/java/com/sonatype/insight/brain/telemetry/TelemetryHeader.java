/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 1.43.0
 */
public class TelemetryHeader
{
  /**
   * Format of the event stream (ex. zip-bundle/1).  Used downstream by a handler to process data associated with
   * this header.
   */
  private String format;

  private String product;

  private String telemetryId;

  private Date createTime;

  private Map<String, String> attributes = new HashMap<>();

  /**
   * Unused constructor required for JSON deserialization support.
   */
  @SuppressWarnings("unused")
  private TelemetryHeader() {
  }

  public TelemetryHeader(final String format, final String product, final Date createTime, final String telemetryId) {
    this.format = format;
    this.product = product;
    this.createTime = createTime;
    this.telemetryId = telemetryId;
  }

  public String getFormat() {
    return format;
  }

  public String getProduct() {
    return product;
  }

  public String getTelemetryId() {
    return telemetryId;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(final Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public Date getCreateTime() {
    return createTime;
  }
}
