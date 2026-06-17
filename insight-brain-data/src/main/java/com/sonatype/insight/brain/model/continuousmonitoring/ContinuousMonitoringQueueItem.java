/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.continuousmonitoring;

import java.util.Date;
import java.util.Objects;
import java.util.StringJoiner;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA entity for the shared continuous monitoring queue. Holds only flow-agnostic execution
 * state (status, priority, retry, worker, timestamps); per-flow identity columns live in the
 * corresponding satellite table (e.g. {@code continuous_monitoring_hosted_repo_item}).
 */
@Entity
@Table(name = "continuous_monitoring_queue")
public class ContinuousMonitoringQueueItem
    implements HasStringId
{
  private static final Logger log = LoggerFactory.getLogger(ContinuousMonitoringQueueItem.class);

  public static final String STATUS_PENDING = "PENDING";

  public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

  public static final long DEFAULT_PRIORITY = 0L;

  public static final int ERROR_MESSAGE_MAX_LENGTH = 500;

  @Id
  @Column(name = "id")
  private String id;

  @Column(name = "flow_type", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  private ContinuousMonitoringFlowType flowType;

  @Column(name = "priority", nullable = false)
  private long priority = DEFAULT_PRIORITY;

  @Column(name = "status", nullable = false, length = 20)
  private String status = STATUS_PENDING;

  @Column(name = "retry_count", nullable = false)
  private int retryCount = 0;

  @Column(name = "create_time", nullable = false)
  private Date createTime;

  @Column(name = "update_time", nullable = false)
  private Date updateTime;

  @Column(name = "acquired_at")
  private Date acquiredAt;

  @Column(name = "worker_id", length = 50)
  private String workerId;

  @Column(name = "error_message", length = ERROR_MESSAGE_MAX_LENGTH)
  private String errorMessage;

  public ContinuousMonitoringQueueItem() {
  }

  public ContinuousMonitoringQueueItem(
      final String id,
      final ContinuousMonitoringFlowType flowType,
      final long priority,
      final Date createTime)
  {
    this.id = id;
    this.flowType = flowType;
    this.priority = priority;
    this.createTime = createTime;
    this.updateTime = createTime;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public ContinuousMonitoringFlowType getFlowType() {
    return flowType;
  }

  public void setFlowType(final ContinuousMonitoringFlowType flowType) {
    this.flowType = flowType;
  }

  public long getPriority() {
    return priority;
  }

  public void setPriority(final long priority) {
    this.priority = priority;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(final int retryCount) {
    this.retryCount = retryCount;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(final Date createTime) {
    this.createTime = createTime;
  }

  public Date getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(final Date updateTime) {
    this.updateTime = updateTime;
  }

  public Date getAcquiredAt() {
    return acquiredAt;
  }

  public void setAcquiredAt(final Date acquiredAt) {
    this.acquiredAt = acquiredAt;
  }

  public String getWorkerId() {
    return workerId;
  }

  public void setWorkerId(final String workerId) {
    this.workerId = workerId;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Sets the error message, truncating to {@code ERROR_MESSAGE_MAX_LENGTH} if necessary.
   * <p>
   * Note: {@code ContinuousMonitoringQueueItemDAO#markRetry} also truncates before persisting, so a
   * message may be truncated twice when written via the DAO. This setter provides a safety net for
   * direct entity mutations. The duplicate truncation is intentional and harmless.
   */
  public void setErrorMessage(final String errorMessage) {
    if (errorMessage != null && errorMessage.length() > ERROR_MESSAGE_MAX_LENGTH) {
      // Log the full message before truncating so the original survives in logs even though the
      // database column is bounded at ERROR_MESSAGE_MAX_LENGTH. id can be null on a freshly
      // constructed entity (no-arg ctor) — render as "<unset>" so logs are unambiguous.
      log.warn("Error message for continuous monitoring queue item {} exceeds {} chars and will be truncated"
          + " in the database; full message follows: {}",
          Objects.toString(id, "<unset>"), ERROR_MESSAGE_MAX_LENGTH, errorMessage);
      this.errorMessage = errorMessage.substring(0, ERROR_MESSAGE_MAX_LENGTH);
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
    ContinuousMonitoringQueueItem that = (ContinuousMonitoringQueueItem) o;
    return id != null ? id.equals(that.id) : that.id == null;
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ContinuousMonitoringQueueItem.class.getSimpleName() + "[", "]")
        .add("id='" + id + "'")
        .add("flowType=" + flowType)
        .add("priority=" + priority)
        .add("status='" + status + "'")
        .add("retryCount=" + retryCount)
        .add("workerId='" + workerId + "'")
        .add("acquiredAt=" + acquiredAt)
        .toString();
  }
}
