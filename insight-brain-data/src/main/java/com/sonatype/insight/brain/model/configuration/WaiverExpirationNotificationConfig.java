/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * Stores waiver expiration notification configuration for an owner
 * (organization, repository manager, or repository container).
 * <p>
 * A missing row for an ownerId means "inherit from parent".
 * {@code notificationDays} is a comma-separated list of day thresholds (e.g. "7,14,30").
 * {@code notificationsJson} is a Jackson-serialized object containing
 * {@code recipientType} (DIRECT, ROLE, or BOTH), {@code directEmails} (list of email addresses),
 * and {@code roleIds} (list of role IDs).
 */
@Entity
@Table(name = "waiver_expiration_notification_config")
public class WaiverExpirationNotificationConfig
    implements HasStringId
{
  @Id
  @Column(name = "waiver_expiration_notification_config_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "notification_days")
  private String notificationDays;

  @Column(name = "notifications_json")
  private String notificationsJson;

  public WaiverExpirationNotificationConfig() {
  }

  public WaiverExpirationNotificationConfig(
      final String ownerId,
      final String notificationDays,
      final String notificationsJson)
  {
    this.ownerId = ownerId;
    this.notificationDays = notificationDays;
    this.notificationsJson = notificationsJson;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(final String ownerId) {
    this.ownerId = ownerId;
  }

  public String getNotificationDays() {
    return notificationDays;
  }

  public void setNotificationDays(final String notificationDays) {
    this.notificationDays = notificationDays;
  }

  public String getNotificationsJson() {
    return notificationsJson;
  }

  public void setNotificationsJson(final String notificationsJson) {
    this.notificationsJson = notificationsJson;
  }
}
