/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "persisted_promote_scan_result")
public class PersistedPromoteScanResult
    implements HasStringId
{
  public enum Status
  {
    PENDING, COMPLETED, FAILED
  }

  @Id
  @Column(name = "persisted_promote_scan_result_id")
  private String id;

  @Column(name = "application_id")
  private String applicationId;

  @Column(name = "status")
  private Status status = Status.PENDING;

  @Column(name = "scan_id")
  private String scanId;

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "create_time")
  private Date createTime = new Date();

  public PersistedPromoteScanResult() {
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public String getScanId() {
    return scanId;
  }

  public void setScanId(String scanId) {
    this.scanId = scanId;
  }

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }
}
