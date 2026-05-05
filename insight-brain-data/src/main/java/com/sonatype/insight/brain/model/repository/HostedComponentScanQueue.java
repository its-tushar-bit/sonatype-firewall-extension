/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import java.util.Date;
import java.util.StringJoiner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA entity for hosted repository component scan job queue.
 * <p>
 * This table queues and tracks scan jobs for hosted repository components with priority-based processing,
 * status tracking, and error handling.
 */
@Entity
@Table(name = "hosted_component_scan_queue")
public class HostedComponentScanQueue
    implements HasStringId
{
  private static final Logger log = LoggerFactory.getLogger(HostedComponentScanQueue.class);

  @Id
  @Column(name = "id")
  private String id;

  @Column(name = "component_id", nullable = false)
  private String componentId;

  @Column(name = "scan_file_id", nullable = false)
  private String scanFileId;

  @Column(name = "status", nullable = false, length = 50)
  private String status;

  public static final int DEFAULT_PRIORITY = 5;

  @Column(name = "priority", nullable = false)
  private Integer priority = DEFAULT_PRIORITY;

  @Column(name = "acquired_at")
  private Date acquiredAt;

  @Column(name = "error_message", length = 2000)
  private String errorMessage;

  @Column(name = "repository_id")
  private String repositoryId;

  @Column(name = "retry_count", nullable = false)
  private int retryCount = 0;

  @Column(name = "purl", length = 2000)
  private String purl;

  @Column(name = "policy_evaluation_stage", length = 50)
  private String policyEvaluationStage;

  public HostedComponentScanQueue() {
  }

  public HostedComponentScanQueue(
      final String componentId,
      final String scanFileId,
      final String status,
      final Integer priority,
      final String repositoryId)
  {
    this.componentId = componentId;
    this.scanFileId = scanFileId;
    this.status = status;
    this.priority = priority != null ? priority : DEFAULT_PRIORITY;
    this.repositoryId = repositoryId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getComponentId() {
    return componentId;
  }

  public void setComponentId(final String componentId) {
    this.componentId = componentId;
  }

  public String getScanFileId() {
    return scanFileId;
  }

  public void setScanFileId(final String scanFileId) {
    this.scanFileId = scanFileId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(final Integer priority) {
    this.priority = priority != null ? priority : DEFAULT_PRIORITY;
  }

  public Date getAcquiredAt() {
    return acquiredAt;
  }

  public void setAcquiredAt(final Date acquiredAt) {
    this.acquiredAt = acquiredAt;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(final String repositoryId) {
    this.repositoryId = repositoryId;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(final int retryCount) {
    this.retryCount = retryCount;
  }

  public String getPurl() {
    return purl;
  }

  public void setPurl(final String purl) {
    this.purl = purl;
  }

  public String getPolicyEvaluationStage() {
    return policyEvaluationStage;
  }

  public void setPolicyEvaluationStage(final String policyEvaluationStage) {
    this.policyEvaluationStage = policyEvaluationStage;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(final String errorMessage) {
    if (errorMessage != null && errorMessage.length() > 2000) {
      log.warn("Error message for component scan queue entry {} exceeds 2000 chars and will be truncated", id);
      this.errorMessage = errorMessage.substring(0, 2000);
    }
    else {
      this.errorMessage = errorMessage;
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    HostedComponentScanQueue that = (HostedComponentScanQueue) o;

    return id != null ? id.equals(that.id) : that.id == null;
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", HostedComponentScanQueue.class.getSimpleName() + "[", "]")
        .add("id='" + id + "'")
        .add("componentId='" + componentId + "'")
        .add("scanFileId='" + scanFileId + "'")
        .add("status='" + status + "'")
        .add("priority=" + priority)
        .add("acquiredAt=" + acquiredAt)
        .add("errorMessage='" + errorMessage + "'")
        .add("repositoryId='" + repositoryId + "'")
        .add("retryCount=" + retryCount)
        .add("purl='" + purl + "'")
        .add("policyEvaluationStage='" + policyEvaluationStage + "'")
        .toString();
  }
}
