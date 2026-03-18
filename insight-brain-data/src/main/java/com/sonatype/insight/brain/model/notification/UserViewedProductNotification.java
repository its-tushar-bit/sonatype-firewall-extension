/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.brain.model.security.User;
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

  /**
   * @since 1.81
   */
  @Column(name = "username_lowercase")
  private String usernameLowercase;

  /**
   * @since 1.81
   */
  @Column(name = "realm_id")
  private String realmId;

  @Column(name = "notification_id")
  private String notificationId;

  public UserViewedProductNotification() {
  }

  public UserViewedProductNotification(final String username, String realmId, final String notificationId) {
    setUsername(username);
    setRealmId(realmId);
    this.notificationId = notificationId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    usernameLowercase = User.normalizeUsername(username);
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

  public String getUsernameLowercase() {
    return usernameLowercase;
  }

  /**
   * This method is defined here only to trick jackson into "thinking" that it de-serialized the value of the
   * usernameLowercase field. If this method is not defined, jackson will set/access the
   * usernameLowercase field directly via reflection, possibly setting it to an incorrect value.
   *
   * @deprecated This method should not be used explicitly.
   */
  @Deprecated
  @SuppressWarnings("unused")
  private void setUsernameLowercase(String usernameLowercase) {
  }

  public String getRealmId() {
    return realmId;
  }

  public void setRealmId(String realmId) {
    this.realmId = realmId;
  }
}
