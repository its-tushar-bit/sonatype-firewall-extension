/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.model;

import java.util.Date;
import java.util.StringJoiner;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "application_count_history")
public class ApplicationCountHistory
    implements HasStringId
{
  @Id
  @Column(name = "application_count_history_id")
  private String id;

  @Column(name = "application_count")
  private int applicationCount;

  @Column(name = "scm_feedback_enabled_count")
  private int scmFeedbackEnabledCount;

  @Column(name = "updated_date")
  private Date updatedDate;

  public ApplicationCountHistory() {
  }

  public ApplicationCountHistory(
      final Date updatedDate,
      final int applicationCount,
      final int scmFeedbackEnabledCount
  )
  {
    this.applicationCount = applicationCount;
    this.updatedDate = updatedDate;
    this.scmFeedbackEnabledCount = scmFeedbackEnabledCount;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public int getApplicationCount() {
    return applicationCount;
  }

  public void setApplicationCount(final int applicationCount) {
    this.applicationCount = applicationCount;
  }

  public void setScmFeedbackEnabledCount(final int scmFeedBackEnabledCount) {
    this.scmFeedbackEnabledCount = scmFeedBackEnabledCount;
  }

  public int getScmFeedbackEnabledCount() {
    return scmFeedbackEnabledCount;
  }

  public Date getUpdatedDate() {
    return updatedDate;
  }

  public void setUpdatedDate(final Date updatedDate) {
    this.updatedDate = updatedDate;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ApplicationCountHistory.class.getSimpleName() + "[", "]")
        .add("id='" + id + "'")
        .add("applicationCount=" + applicationCount)
        .add("updatedDate=" + updatedDate)
        .add("scmFeedbackEnabledCount=" + scmFeedbackEnabledCount)
        .toString();
  }
}
