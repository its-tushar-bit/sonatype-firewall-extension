/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.model;

import java.util.Date;
import java.util.StringJoiner;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

  @Column(name = "policy_action_failures_by_app_count")
  private int policyActionFailuresByAppCount;

  @Column(name = "waivers_count")
  private int waiversCount;

  @Column(name = "mean_time_to_remediate_ms")
  private long meanTimeToRemediateMs;

  @Column(name = "updated_date")
  private Date updatedDate;

  public ApplicationCountHistory() {
  }

  public ApplicationCountHistory(
      final Date updatedDate,
      final int applicationCount,
      final int scmFeedbackEnabledCount,
      final int policyActionFailuresByAppCount,
      final int waiversCount,
      final long meanTimeToRemediateMs)
  {
    this.applicationCount = applicationCount;
    this.updatedDate = updatedDate;
    this.scmFeedbackEnabledCount = scmFeedbackEnabledCount;
    this.policyActionFailuresByAppCount = policyActionFailuresByAppCount;
    this.waiversCount = waiversCount;
    this.meanTimeToRemediateMs = meanTimeToRemediateMs;
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

  public int getPolicyActionFailuresByAppCount() {
    return policyActionFailuresByAppCount;
  }

  public void setPolicyActionFailuresByAppCount(final int policyActionFailuresByAppCount) {
    this.policyActionFailuresByAppCount = policyActionFailuresByAppCount;
  }

  public int getWaiversCount() {
    return waiversCount;
  }

  public void setWaiversCount(final int waiversCount) {
    this.waiversCount = waiversCount;
  }

  public long getMeanTimeToRemediateMs() {
    return meanTimeToRemediateMs;
  }

  public void setMeanTimeToRemediateMs(final long meanTimeToRemediateMs) {
    this.meanTimeToRemediateMs = meanTimeToRemediateMs;
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
        .add("policyActionFailuresByAppCount=" + policyActionFailuresByAppCount)
        .add("waiversCount=" + waiversCount)
        .add("meanTimeToRemediateMs=" + meanTimeToRemediateMs)
        .toString();
  }
}
