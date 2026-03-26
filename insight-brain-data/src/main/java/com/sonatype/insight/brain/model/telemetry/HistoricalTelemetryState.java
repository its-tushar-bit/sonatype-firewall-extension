/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.telemetry;

import java.util.Date;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "historical_telemetry_state")
public class HistoricalTelemetryState
    implements HasStringId
{
  @Id
  @Column(name = "historical_telemetry_state_id")
  private String id;

  @Column(name = "batch_size")
  private int batchSize = 1000;

  @Column(name = "created")
  private Date created;

  @Column(name = "cutoff_date")
  private Date cutoffDate;

  @Column(name = "last_record_key")
  private String lastRecordKey;

  @Column(name = "last_record_time")
  private Date lastRecordTime;

  @Column(name = "last_updated")
  private Date lastUpdated;

  @Column(name = "min_free_memory_mb")
  private int minFreeMemoryMb = 10;

  @Column(name = "start_time")
  private Date startTime;

  @Column(name = "status")
  private String status;

  @Column(name = "retry_count")
  private int retryCount = 0;

  @Column(name = "last_retry_time")
  private Date lastRetryTime;

  // Getters and Setters

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public Date getCutoffDate() {
    return cutoffDate;
  }

  public void setCutoffDate(Date cutoffDate) {
    this.cutoffDate = cutoffDate;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public Date getLastRecordTime() {
    return lastRecordTime;
  }

  public void setLastRecordTime(Date lastRecordTime) {
    this.lastRecordTime = lastRecordTime;
  }

  public String getLastRecordKey() {
    return lastRecordKey;
  }

  public void setLastRecordKey(String lastRecordKey) {
    this.lastRecordKey = lastRecordKey;
  }

  public int getMinFreeMemoryMb() {
    return minFreeMemoryMb;
  }

  public void setMinFreeMemoryMb(final int minFreeMemoryMb) {
    this.minFreeMemoryMb = minFreeMemoryMb;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(int retryCount) {
    this.retryCount = retryCount;
  }

  public Date getLastRetryTime() {
    return lastRetryTime;
  }

  public void setLastRetryTime(Date lastRetryTime) {
    this.lastRetryTime = lastRetryTime;
  }
}
