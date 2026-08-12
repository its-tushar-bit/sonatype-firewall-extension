/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.Date;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "search_index_change")
public class SearchIndexChange
    implements HasStringId
{
  public enum ChangeType
  {
    APPLICATION,

    ORGANIZATION,

    POLICY,

    LAST_POLICY_EVALUATION,

    APPLICATION_CATEGORY,

    LABEL,

    SBOM,

    // One change type covers both manual (policy_waiver) and auto (auto_policy_waiver) waivers.
    // changeData encodes the waiver kind with a prefix so the updater resolves the correct table
    // directly: "MANUAL:<id>" for policy_waiver, "AUTO:<id>" for auto_policy_waiver (mirroring the
    // SBOM "appId:version" split). The raw id follows the prefix.
    POLICY_WAIVER,

    // Waiver-request change; changeData is the raw policy_waiver_request id (no prefix — a single
    // table). Indexed as ItemType.POLICY_WAIVER_REQUEST, distinct from POLICY_WAIVER.
    POLICY_WAIVER_REQUEST
  }

  // changeData prefixes for POLICY_WAIVER changes (see ChangeType.POLICY_WAIVER).
  public static final String POLICY_WAIVER_MANUAL_PREFIX = "MANUAL:";

  public static final String POLICY_WAIVER_AUTO_PREFIX = "AUTO:";

  @Id
  @Column(name = "search_index_change_id")
  private String id;

  @Column(name = "change_type")
  @Enumerated(EnumType.STRING)
  private ChangeType changeType;

  @Column(name = "change_data")
  private String changeData;

  @Column(name = "created_at")
  private Date createdAt;

  @Column(name = "status")
  private String status;

  /**
   * Retry bookkeeping written when the indexer parks a change it could not apply. Nothing reads these
   * yet: the worker that reprocesses parked changes arrives with CLM-44498, and until then they exist
   * so a failure is diagnosable from the row rather than only from a log line.
   */
  @Column(name = "attempt_count")
  private Integer attemptCount;

  @Column(name = "last_error")
  private String lastError;

  @Column(name = "available_at")
  private Date availableAt;

  @Transient
  private boolean processed;

  public static final String STATUS_PENDING = "PENDING";

  public static final String STATUS_PROCESSING = "PROCESSING";

  public static final String STATUS_FAILED = "FAILED";

  public SearchIndexChange() {
  }

  public SearchIndexChange(ChangeType changeType, String changeData) {
    setChangeType(changeType);
    setChangeData(changeData);
    Date now = new Date();
    setCreatedAt(now);
    setStatus(STATUS_PENDING);
    setAttemptCount(0);
    setAvailableAt(now);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public ChangeType getChangeType() {
    return changeType;
  }

  public void setChangeType(ChangeType changeType) {
    this.changeType = changeType;
  }

  public String getChangeData() {
    return changeData;
  }

  public void setChangeData(String changeData) {
    this.changeData = changeData;
  }

  @Override
  public String toString() {
    return getChangeType() + "/" + getChangeData();
  }

  public boolean isProcessed() {
    return processed;
  }

  public void setProcessed(final boolean processed) {
    this.processed = processed;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final Date createdAt) {
    this.createdAt = createdAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(final String status) {
    this.status = status;
  }

  public Integer getAttemptCount() {
    return attemptCount;
  }

  public void setAttemptCount(final Integer attemptCount) {
    this.attemptCount = attemptCount;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(final String lastError) {
    this.lastError = lastError;
  }

  public Date getAvailableAt() {
    return availableAt;
  }

  public void setAvailableAt(final Date availableAt) {
    this.availableAt = availableAt;
  }
}
