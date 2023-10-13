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
  public ApplicationCountHistory() {
  }

  public ApplicationCountHistory(final int applicationCount, Date updatedDate) {
    this.applicationCount = applicationCount;
    this.updatedDate = updatedDate;
  }

  @Id
  @Column(name = "application_count_history_id")
  private String id;

  @Column(name = "application_count")
  private int applicationCount;

  @Column(name = "updated_date")
  private Date updatedDate;

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
        .toString();
  }
}
