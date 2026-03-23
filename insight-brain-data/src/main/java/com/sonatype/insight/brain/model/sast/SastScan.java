/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sast;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

  @Column(name = "sast_scm_scan_context_id")
  private String sastScmScanContextId;

  @Column(name = "created_at")
  private Date createdAt;

  public SastScan() {
  }

  public SastScan(final String applicationId) {
    this.applicationId = applicationId;
    this.createdAt = new Date();
  }

  public SastScan(final String applicationId, final String sastScmScanContextId) {
    this(applicationId);
    this.sastScmScanContextId = sastScmScanContextId;
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

  public String getSastScmScanContextId() {
    return sastScmScanContextId;
  }

  public void setSastScmScanContextId(String sastScmScanContextId) {
    this.sastScmScanContextId = sastScmScanContextId;
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
        ", sastScmScanContextId='" + sastScmScanContextId + '\'' +
        ", createdAt=" + createdAt +
        '}';
  }
}
