/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.notification;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.14.0
 */
@Entity
@Table(name = "user_viewed_product_notification")
public class UserViewedProductNotification
    implements HasStringId
{
  @Id
  @Column(name = "user_viewed_product_notification_id")
  private String id;

  @Column(name = "username")
  private String username;

  @Column(name = "notification_id")
  private String notificationId;

  public UserViewedProductNotification() {
  }

  public UserViewedProductNotification(final String username, final String notificationId) {
    this.username = username;
    this.notificationId = notificationId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getNotificationId() {
    return notificationId;
  }

  public void setNotificationId(final String notificationId) {
    this.notificationId = notificationId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }
}
