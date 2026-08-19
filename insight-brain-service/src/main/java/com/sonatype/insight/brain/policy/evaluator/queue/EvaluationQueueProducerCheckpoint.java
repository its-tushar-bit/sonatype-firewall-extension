/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator.queue;

import java.util.Date;

public class EvaluationQueueProducerCheckpoint
{
  private Date startTime;

  private String applicationId;

  private int latestOffset;

  private int maxAppActiveSboms;

  public EvaluationQueueProducerCheckpoint() {
    // for Jackson
  }

  public EvaluationQueueProducerCheckpoint(
      final Date startTime,
      final String applicationId,
      final int latestOffset,
      final int maxAppActiveSboms)
  {
    this.startTime = startTime;
    this.applicationId = applicationId;
    this.latestOffset = latestOffset;
    this.maxAppActiveSboms = maxAppActiveSboms;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(final Date startTime) {
    this.startTime = startTime;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(final String applicationId) {
    this.applicationId = applicationId;
  }

  public int getLatestOffset() {
    return latestOffset;
  }

  public void setLatestOffset(final int latestOffset) {
    this.latestOffset = latestOffset;
  }

  public int getMaxAppActiveSboms() {
    return maxAppActiveSboms;
  }

  public void setMaxAppActiveSboms(final int maxAppActiveSboms) {
    this.maxAppActiveSboms = maxAppActiveSboms;
  }

  public boolean isComplete() {
    return getLatestOffset() >= getMaxAppActiveSboms();
  }
}
