/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.relay;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * One processed relay event. Used by {@code RelayEventDeduplicator} to drop redeliveries
 * (primary key {@code event_id}) and to suppress logically-equivalent events that arrive with
 * a different UUID during the cutover from legacy SCM polling
 * (secondary key {@code (application_public_id, pull_request_number, commit_hash, mode)}).
 *
 * <p>
 * {@code mode} is the relay registration kind ("pat" or "github-app") at the time the event
 * was processed. Including it in the secondary key prevents over-matching after a customer
 * migrates between modes: an event arriving in the new mode against a PR whose old-mode row
 * still lives in the dedup window is no longer treated as a secondary duplicate.
 */
@Entity
@Table(name = "relay_event_log")
public class RelayEventLog
    implements HasStringId
{
  @Id
  @Column(name = "relay_event_log_id")
  private String id;

  @Column(name = "event_id")
  private String eventId;

  @Column(name = "application_public_id")
  private String applicationPublicId;

  @Column(name = "pull_request_number")
  private Integer pullRequestNumber;

  @Column(name = "commit_hash")
  private String commitHash;

  @Column(name = "event_type")
  private String eventType;

  @Column(name = "processed_at")
  private Date processedAt;

  @Column(name = "mode")
  private String mode;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public String getApplicationPublicId() {
    return applicationPublicId;
  }

  public void setApplicationPublicId(String applicationPublicId) {
    this.applicationPublicId = applicationPublicId;
  }

  public Integer getPullRequestNumber() {
    return pullRequestNumber;
  }

  public void setPullRequestNumber(Integer pullRequestNumber) {
    this.pullRequestNumber = pullRequestNumber;
  }

  public String getCommitHash() {
    return commitHash;
  }

  public void setCommitHash(String commitHash) {
    this.commitHash = commitHash;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public Date getProcessedAt() {
    return processedAt;
  }

  public void setProcessedAt(Date processedAt) {
    this.processedAt = processedAt;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }
}
