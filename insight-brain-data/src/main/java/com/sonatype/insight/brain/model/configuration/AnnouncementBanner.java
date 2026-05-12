/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * Deployment-global announcement banner (used for maintenance windows, product notices, etc.). Stored as a
 * singleton row in the DataMart schema, which resides in the {@code global} schema under MTIQ.
 */
@Entity
@Table(name = "announcement_banner")
public class AnnouncementBanner
    implements HasStringId
{
  @Id
  @Column(name = "announcement_banner_id")
  private String id;

  @Column(name = "enabled")
  private boolean enabled;

  @Column(name = "window_id")
  private String windowId;

  @Column(name = "display_from")
  private OffsetDateTime displayFrom;

  @Column(name = "display_until")
  private OffsetDateTime displayUntil;

  @Column(name = "message")
  private String message;

  @Column(name = "severity")
  private String severity;

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public String getWindowId() {
    return windowId;
  }

  public void setWindowId(final String windowId) {
    this.windowId = windowId;
  }

  public OffsetDateTime getDisplayFrom() {
    return displayFrom;
  }

  public void setDisplayFrom(final OffsetDateTime displayFrom) {
    this.displayFrom = displayFrom;
  }

  public OffsetDateTime getDisplayUntil() {
    return displayUntil;
  }

  public void setDisplayUntil(final OffsetDateTime displayUntil) {
    this.displayUntil = displayUntil;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(final String severity) {
    this.severity = severity;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(final OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
