/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.searchindex;

import java.util.Date;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Singleton ({@link #CURRENT_ID}) health row maintained by index workers. Analyze APIs read by PK only.
 */
@Entity
@Table(name = "search_index_health")
public class SearchIndexHealth
    implements HasStringId
{
  public static final String CURRENT_ID = "CURRENT";

  public static final String STATUS_HEALTHY = "HEALTHY";

  public static final String STATUS_WARNING = "WARNING";

  public static final String STATUS_NOT_HEALTHY = "NOT_HEALTHY";

  public static final String STATUS_REBUILD_IN_PROGRESS = "REBUILD_IN_PROGRESS";

  public static final String OP_NONE = "NONE";

  public static final String OP_POINT_REPAIR = "POINT_REPAIR";

  public static final String OP_SCOPED_CLEANUP = "SCOPED_CLEANUP";

  public static final String OP_FULL_REBUILD = "FULL_REBUILD";

  public static final String OP_ATTRIBUTE_BACKFILL = "ATTRIBUTE_BACKFILL";

  public static final String UNLOCK_NOT_STARTED = "NOT_STARTED";

  public static final String UNLOCK_INDEXING = "INDEXING";

  public static final String UNLOCK_READY_TO_TEST = "READY_TO_TEST";

  public static final String UNLOCK_UNLOCKED = "UNLOCKED";

  @Id
  @Column(name = "search_index_health_id")
  private String id = CURRENT_ID;

  @Column(name = "health_status", nullable = false)
  private String healthStatus = STATUS_HEALTHY;

  @Column(name = "recommended_op")
  private String recommendedOp = OP_NONE;

  @Column(name = "queue_lag_seconds", nullable = false)
  private long queueLagSeconds;

  @Column(name = "pending_change_count", nullable = false)
  private long pendingChangeCount;

  @Column(name = "failed_change_count", nullable = false)
  private long failedChangeCount;

  @Column(name = "failed_change_window_start")
  private Date failedChangeWindowStart;

  @Column(name = "oldest_pending_created_at")
  private Date oldestPendingCreatedAt;

  @Column(name = "serving_generation_id")
  private String servingGenerationId;

  @Column(name = "active_job_id")
  private String activeJobId;

  @Column(name = "noux_unlock_state", nullable = false)
  private String nouxUnlockState = UNLOCK_NOT_STARTED;

  @Column(name = "last_successful_cutover_at")
  private Date lastSuccessfulCutoverAt;

  @Column(name = "last_cleanup_at")
  private Date lastCleanupAt;

  @Column(name = "updated_at", nullable = false)
  private Date updatedAt;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getHealthStatus() {
    return healthStatus;
  }

  public void setHealthStatus(final String healthStatus) {
    this.healthStatus = healthStatus;
  }

  public String getRecommendedOp() {
    return recommendedOp;
  }

  public void setRecommendedOp(final String recommendedOp) {
    this.recommendedOp = recommendedOp;
  }

  public long getQueueLagSeconds() {
    return queueLagSeconds;
  }

  public void setQueueLagSeconds(final long queueLagSeconds) {
    this.queueLagSeconds = queueLagSeconds;
  }

  public long getPendingChangeCount() {
    return pendingChangeCount;
  }

  public void setPendingChangeCount(final long pendingChangeCount) {
    this.pendingChangeCount = pendingChangeCount;
  }

  public long getFailedChangeCount() {
    return failedChangeCount;
  }

  public void setFailedChangeCount(final long failedChangeCount) {
    this.failedChangeCount = failedChangeCount;
  }

  public Date getFailedChangeWindowStart() {
    return failedChangeWindowStart;
  }

  public void setFailedChangeWindowStart(final Date failedChangeWindowStart) {
    this.failedChangeWindowStart = failedChangeWindowStart;
  }

  public Date getOldestPendingCreatedAt() {
    return oldestPendingCreatedAt;
  }

  public void setOldestPendingCreatedAt(final Date oldestPendingCreatedAt) {
    this.oldestPendingCreatedAt = oldestPendingCreatedAt;
  }

  public String getServingGenerationId() {
    return servingGenerationId;
  }

  public void setServingGenerationId(final String servingGenerationId) {
    this.servingGenerationId = servingGenerationId;
  }

  public String getActiveJobId() {
    return activeJobId;
  }

  public void setActiveJobId(final String activeJobId) {
    this.activeJobId = activeJobId;
  }

  public String getNouxUnlockState() {
    return nouxUnlockState;
  }

  public void setNouxUnlockState(final String nouxUnlockState) {
    this.nouxUnlockState = nouxUnlockState;
  }

  public Date getLastSuccessfulCutoverAt() {
    return lastSuccessfulCutoverAt;
  }

  public void setLastSuccessfulCutoverAt(final Date lastSuccessfulCutoverAt) {
    this.lastSuccessfulCutoverAt = lastSuccessfulCutoverAt;
  }

  public Date getLastCleanupAt() {
    return lastCleanupAt;
  }

  public void setLastCleanupAt(final Date lastCleanupAt) {
    this.lastCleanupAt = lastCleanupAt;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(final Date updatedAt) {
    this.updatedAt = updatedAt;
  }
}
