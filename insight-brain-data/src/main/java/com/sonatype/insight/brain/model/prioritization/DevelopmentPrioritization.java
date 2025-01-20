/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.prioritization;

import java.util.Date;
import java.util.Objects;
import java.util.StringJoiner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "development_prioritization")
public class DevelopmentPrioritization
    implements HasStringId
{
  @Id
  @Column(name = "development_prioritization_id")
  private String id;

  @Column(name = "scan_id")
  private String scanId;

  @Column(name = "created_at")
  private Date createdAt;

  @Column(name = "updated_at")
  private Date updatedAt;

  public DevelopmentPrioritization() {
  }

  public DevelopmentPrioritization(
      final String scanId)
  {
    this.scanId = scanId;
    this.updatedAt = this.createdAt = new Date();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getScanId() {
    return scanId;
  }

  public void setScanId(final String scanId) {
    this.scanId = scanId;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final Date createdAt) {
    this.createdAt = createdAt;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(final Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", DevelopmentPrioritization.class.getSimpleName() + "[", "]")
        .add("id='" + id + "'")
        .add("scanId='" + scanId + "'")
        .add("createdAt=" + createdAt)
        .add("updatedAt=" + updatedAt)
        .toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DevelopmentPrioritization that = (DevelopmentPrioritization) o;
    return Objects.equals(scanId, that.scanId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scanId);
  }
}
