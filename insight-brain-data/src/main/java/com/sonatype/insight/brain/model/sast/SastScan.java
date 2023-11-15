/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sast;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "sast_scan")
public class SastScan
    implements HasStringId
{
  @Id
  @Column(name = "sast_scan_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "created_at")
  private Date createdAt;

  public SastScan(final String applicationId) {
    this.applicationId = applicationId;
    this.createdAt = new Date();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(final String applicationId) {
    this.applicationId = applicationId;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final Date createdAt) {
    this.createdAt = createdAt;
  }

  @Override
  public String toString() {
    return "SastScan{" +
        "id='" + id + '\'' +
        ", applicationId='" + applicationId + '\'' +
        ", createdAt=" + createdAt +
        '}';
  }
}
