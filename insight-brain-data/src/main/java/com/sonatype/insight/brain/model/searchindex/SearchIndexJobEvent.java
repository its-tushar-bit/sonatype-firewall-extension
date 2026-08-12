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

@Entity
@Table(name = "search_index_job_event")
public class SearchIndexJobEvent
    implements HasStringId
{
  public static final String SEVERITY_INFO = "INFO";

  public static final String SEVERITY_WARN = "WARN";

  public static final String SEVERITY_ERROR = "ERROR";

  @Id
  @Column(name = "search_index_job_event_id")
  private String id;

  @Column(name = "search_index_job_id", nullable = false)
  private String searchIndexJobId;

  @Column(name = "seq", nullable = false)
  private long seq;

  @Column(name = "severity", nullable = false)
  private String severity = SEVERITY_INFO;

  @Column(name = "event_code", nullable = false)
  private String eventCode;

  @Column(name = "message", nullable = false)
  private String message;

  @Column(name = "created_at", nullable = false)
  private Date createdAt;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getSearchIndexJobId() {
    return searchIndexJobId;
  }

  public void setSearchIndexJobId(final String searchIndexJobId) {
    this.searchIndexJobId = searchIndexJobId;
  }

  public long getSeq() {
    return seq;
  }

  public void setSeq(final long seq) {
    this.seq = seq;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(final String severity) {
    this.severity = severity;
  }

  public String getEventCode() {
    return eventCode;
  }

  public void setEventCode(final String eventCode) {
    this.eventCode = eventCode;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final Date createdAt) {
    this.createdAt = createdAt;
  }
}
