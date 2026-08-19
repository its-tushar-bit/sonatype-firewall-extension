/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.searchindex;

import java.util.Date;
import java.util.Set;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "search_index_job")
public class SearchIndexJob
    implements HasStringId
{
  public static final String TYPE_FULL_REBUILD = "FULL_REBUILD";

  public static final String TYPE_FIRST_TIME_INDEX = "FIRST_TIME_INDEX";

  public static final String TYPE_SCOPED_CLEANUP = "SCOPED_CLEANUP";

  public static final String TYPE_POINT_REPAIR = "POINT_REPAIR";

  public static final String TYPE_ATTRIBUTE_BACKFILL = "ATTRIBUTE_BACKFILL";

  public static final String TRIGGER_UNLOCK_WIZARD = "UNLOCK_WIZARD";

  public static final String TRIGGER_HEALTH_UI = "HEALTH_UI";

  public static final String TRIGGER_SUPPORT = "SUPPORT";

  public static final String TRIGGER_SYSTEM = "SYSTEM";

  public static final String STATUS_PENDING = "PENDING";

  public static final String STATUS_RUNNING = "RUNNING";

  public static final String STATUS_CANCELLING = "CANCELLING";

  public static final String STATUS_SUCCEEDED = "SUCCEEDED";

  public static final String STATUS_FAILED = "FAILED";

  public static final String STATUS_CANCELLED = "CANCELLED";

  /**
   * Statuses that occupy the single active-job slot. {@link #isActive()} and the DAO predicate that
   * finds the active job must agree, or {@code findActiveJob} returns rows that then test as inactive.
   */
  public static final Set<String> ACTIVE_STATUSES = Set.of(STATUS_PENDING, STATUS_RUNNING, STATUS_CANCELLING);

  public static final Set<String> TRIGGERS =
      Set.of(TRIGGER_UNLOCK_WIZARD, TRIGGER_HEALTH_UI, TRIGGER_SUPPORT, TRIGGER_SYSTEM);

  /**
   * Types driven by the full-rebuild engine rather than by a maintenance worker. Start, cancel and
   * completion all have to agree on this set: a type that can start but whose cancel path does not
   * reach the engine, or whose completion hook declines to close it, holds the single active slot
   * until someone edits the database.
   */
  public static final Set<String> REBUILD_TYPES = Set.of(TYPE_FULL_REBUILD, TYPE_FIRST_TIME_INDEX);

  public static boolean isRebuildType(final String jobType) {
    return REBUILD_TYPES.contains(jobType);
  }

  /**
   * Value held in {@code active_slot} while a job occupies the single active slot. The column carries a
   * plain UNIQUE constraint and is null once a job is terminal, which both Postgres and H2 exclude from
   * uniqueness. That gives the one-active-job invariant a database backstop across nodes, where the JVM
   * lock in the service only covers one. A filtered unique index would be the direct expression of this
   * but H2 does not support one, and the two dialects share a schema.
   */
  public static final String ACTIVE_SLOT = "ACTIVE";

  @Id
  @Column(name = "search_index_job_id")
  private String id;

  @Column(name = "job_type", nullable = false)
  private String jobType;

  @Column(name = "trigger", nullable = false)
  private String trigger;

  @Column(name = "status", nullable = false)
  private String status = STATUS_PENDING;

  @Column(name = "active_slot")
  private String activeSlot = ACTIVE_SLOT;

  @Column(name = "progress_percent", nullable = false)
  private short progressPercent;

  @Column(name = "phase")
  private String phase;

  @Column(name = "eta_finish_at")
  private Date etaFinishAt;

  @Column(name = "started_at")
  private Date startedAt;

  @Column(name = "finished_at")
  private Date finishedAt;

  @Column(name = "cancel_requested_at")
  private Date cancelRequestedAt;

  @Column(name = "building_generation_id")
  private String buildingGenerationId;

  @Column(name = "serving_generation_id_at_start")
  private String servingGenerationIdAtStart;

  @Column(name = "recommended_op")
  private String recommendedOp;

  @Column(name = "error_code")
  private String errorCode;

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "created_by_user_id")
  private String createdByUserId;

  @Column(name = "created_at", nullable = false)
  private Date createdAt;

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

  public String getJobType() {
    return jobType;
  }

  public void setJobType(final String jobType) {
    this.jobType = jobType;
  }

  public String getTrigger() {
    return trigger;
  }

  public void setTrigger(final String trigger) {
    this.trigger = trigger;
  }

  public String getStatus() {
    return status;
  }

  /**
   * Also derives {@code active_slot}, so the uniquely-constrained column cannot disagree with the status
   * it is meant to describe. Every transition goes through here, including hydration from a row.
   */
  public void setStatus(final String status) {
    this.status = status;
    this.activeSlot = isActive() ? ACTIVE_SLOT : null;
  }

  public String getActiveSlot() {
    return activeSlot;
  }

  public void setActiveSlot(final String activeSlot) {
    this.activeSlot = activeSlot;
  }

  public short getProgressPercent() {
    return progressPercent;
  }

  public void setProgressPercent(final short progressPercent) {
    this.progressPercent = progressPercent;
  }

  public String getPhase() {
    return phase;
  }

  public void setPhase(final String phase) {
    this.phase = phase;
  }

  public Date getEtaFinishAt() {
    return etaFinishAt;
  }

  public void setEtaFinishAt(final Date etaFinishAt) {
    this.etaFinishAt = etaFinishAt;
  }

  public Date getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(final Date startedAt) {
    this.startedAt = startedAt;
  }

  public Date getFinishedAt() {
    return finishedAt;
  }

  public void setFinishedAt(final Date finishedAt) {
    this.finishedAt = finishedAt;
  }

  public Date getCancelRequestedAt() {
    return cancelRequestedAt;
  }

  public void setCancelRequestedAt(final Date cancelRequestedAt) {
    this.cancelRequestedAt = cancelRequestedAt;
  }

  public String getBuildingGenerationId() {
    return buildingGenerationId;
  }

  public void setBuildingGenerationId(final String buildingGenerationId) {
    this.buildingGenerationId = buildingGenerationId;
  }

  public String getServingGenerationIdAtStart() {
    return servingGenerationIdAtStart;
  }

  public void setServingGenerationIdAtStart(final String servingGenerationIdAtStart) {
    this.servingGenerationIdAtStart = servingGenerationIdAtStart;
  }

  public String getRecommendedOp() {
    return recommendedOp;
  }

  public void setRecommendedOp(final String recommendedOp) {
    this.recommendedOp = recommendedOp;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(final String errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(final String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getCreatedByUserId() {
    return createdByUserId;
  }

  public void setCreatedByUserId(final String createdByUserId) {
    this.createdByUserId = createdByUserId;
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

  public boolean isActive() {
    return status != null && ACTIVE_STATUSES.contains(status);
  }
}
