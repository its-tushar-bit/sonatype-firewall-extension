/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.thirdpartyscans;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import com.sonatype.insight.json.store.ApiDateFormat;

public class RecentVulnerabilitiesDTO
{
  private String refId;

  private double severity;

  private String severityStatus;

  @ApiDateFormat
  private Date createdAt;

  public RecentVulnerabilitiesDTO() {
    // for Jackson
  }

  public RecentVulnerabilitiesDTO(Object[] array) {
    refId = (String) array[0];
    severity = (double) array[1];
    severityStatus = String.valueOf(array[2]);
    createdAt = toDate(array[3]);
  }

  private static Date toDate(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Date) {
      return (Date) value;
    }
    if (value instanceof LocalDateTime) {
      return Date.from(((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant());
    }
    throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to Date");
  }

  public String getRefId() {
    return refId;
  }

  public String getSeverityStatus() {
    return severityStatus;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setRefId(final String refId) {
    this.refId = refId;
  }

  public void setSeverityStatus(final String severityStatus) {
    this.severityStatus = severityStatus;
  }

  public void setCreatedAt(final Date createdAt) {
    this.createdAt = createdAt;
  }

  public double getSeverity() {
    return severity;
  }

  public void setSeverity(final double severity) {
    this.severity = severity;
  }
}
