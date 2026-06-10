/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.innersource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "innersource_cleanup_pending")
public class InnerSourceCleanupPending
    implements HasStringId
{
  @Id
  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "last_scan_id")
  private String lastScanId;

  public InnerSourceCleanupPending() {
  }

  public InnerSourceCleanupPending(String applicationId, String lastScanId) {
    this.applicationId = applicationId;
    this.lastScanId = lastScanId;
  }

  @Override
  public String getId() {
    return applicationId;
  }

  @Override
  public void setId(final String id) {
    this.applicationId = id;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(final String applicationId) {
    this.applicationId = applicationId;
  }

  public String getLastScanId() {
    return lastScanId;
  }

  public void setLastScanId(final String lastScanId) {
    this.lastScanId = lastScanId;
  }
}
