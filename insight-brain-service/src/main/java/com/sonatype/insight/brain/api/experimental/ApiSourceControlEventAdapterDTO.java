/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Date;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

public final class ApiSourceControlEventAdapterDTO
{
  private ApiSourceControlEventAdapterDTO() {
  }

  public static ApiSourceControlEventDTO convert(SourceControlEvent sourceControlEvent) {
    ApiSourceControlEventDTO result = new ApiSourceControlEventDTO();
    result.setId(sourceControlEvent.getId());
    result.setUser(sourceControlEvent.getScmUsername());
    result.setApplicationId(sourceControlEvent.getApplicationId());
    result.setType(sourceControlEvent.getEventType());
    result.setPriority(sourceControlEvent.getEventPriority());
    result.setStatus(sourceControlEvent.getEventStatus());
    // Do not expose the status/error details, they may contain sensitive data.
    // This was flagged in a pen test.
    // See https://sonatype.atlassian.net/browse/CLM-29901 for details.
    // result.setStatusDetails(sourceControlEvent.getEventStatusDetails());
    // result.setErrorDetails(sourceControlEvent.getEventErrorDetails());
    result.setCreateTime(sourceControlEvent.getCreateTime());
    result.setStartTime(sourceControlEvent.getStartTime());
    result.setCompleteTime(sourceControlEvent.getCompleteTime());
    result.setTimeWaiting(dateDifference(result.getStartTime(), result.getCreateTime()));
    result.setTimeExecuting(dateDifference(result.getStartTime(), result.getCompleteTime()));
    return result;
  }

  private static Long dateDifference(Date start, Date end) {
    if (start == null) {
      return null;
    }
    else if (end == null) {
      return Math.abs(System.currentTimeMillis() - start.getTime());
    }
    return Math.abs(end.getTime() - start.getTime());
  }
}
