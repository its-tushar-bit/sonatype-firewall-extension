/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "source_control_user_activity")
public class SourceControlUserActivity
    implements HasStringId
{
  @Id
  @Column(name = "source_control_user_activity_id")
  private String id;

  @Column(name = "source_control_user_id")
  private String sourceControlUserId;

  @Column(name = "commit_year_month", columnDefinition = "TIMESTAMP")
  private LocalDate commitYearMonth;

  @Column(name = "is_sent_to_telemetry")
  private boolean isSentToTelemetry;

  public SourceControlUserActivity() {
  }

  public SourceControlUserActivity(final String sourceControlUserId, final LocalDate commitYearMonth) {
    this.sourceControlUserId = sourceControlUserId;
    this.commitYearMonth = commitYearMonth;
  }

  public String getSourceControlUserId() {
    return sourceControlUserId;
  }

  public void setSourceControlUserId(String sourceControlUserId) {
    this.sourceControlUserId = sourceControlUserId;
  }

  public LocalDate getCommitYearMonth() {
    return commitYearMonth;
  }

  public void setCommitYearMonth(LocalDate commitYearMonth) {
    this.commitYearMonth = commitYearMonth;
  }

  public boolean isSentToTelemetry() {
    return isSentToTelemetry;
  }

  public void setSentToTelemetry(boolean sentToTelemetry) {
    isSentToTelemetry = sentToTelemetry;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }
}
