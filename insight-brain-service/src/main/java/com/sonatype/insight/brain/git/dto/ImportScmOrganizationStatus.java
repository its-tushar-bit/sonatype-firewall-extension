/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.dto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImportScmOrganizationStatus
{
  public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

  public ImportScmOrganizationRequest request;

  public String errors;

  public String status;

  public long importSuccessCount;

  public String startTime;

  public String lastUpdatedTime;

  public long importFailureCount;

  public ImportScmOrganizationStatus() {
    // no-op
  }

  public ImportScmOrganizationStatus(
      final ImportScmOrganizationRequest request,
      final String status,
      final Integer importSuccessCount,
      final Integer importFailureCount)
  {

    this.request = request;
    this.status = status;
    this.importSuccessCount = importSuccessCount;
    this.importFailureCount = importFailureCount;
  }

  public void updateStartTime(Date startTime) {
    this.startTime = DATE_FORMAT.format(startTime);
  }

  public void updateLastUpdatedTime(Date lastUpdatedTime) {
    this.lastUpdatedTime = DATE_FORMAT.format(lastUpdatedTime);
  }

  public Date startTimeAsDate() {
    return parseToDate(startTime);
  }

  public Date lastUpdatedTimeAsDate() {
    return parseToDate(lastUpdatedTime);
  }

  private Date parseToDate(String dateTimeString) {
    try {
      return DATE_FORMAT.parse(dateTimeString);
    }
    catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }
}
