/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.notification;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.14.0
 */
@Named
@Singleton
public class UserViewedProductNotificationDAO
    extends AbstractOperationalSqlDAO<UserViewedProductNotification>
{
  @Inject
  public UserViewedProductNotificationDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public UserViewedProductNotification getByUsernameAndRealmIdAndNotificationId(
      String username,
      String realmId,
      String notificationId)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsernameAndRealmIdAndNotificationId(tx, username, realmId, notificationId);
    }
  }

  public UserViewedProductNotification getByUsernameAndRealmIdAndNotificationId(
      TransactionContext tx,
      String username,
      String realmId,
      String notificationId)
  {
    username = User.normalizeUsername(username);
    String sQuery = "SELECT entity FROM UserViewedProductNotification entity" + //
        " WHERE entity.usernameLowercase=?1 AND entity.realmId=?2 AND entity.notificationId=?3";

    return get(tx, sQuery, username, realmId, notificationId);
  }

  @Override
  public void insert(TransactionContext tx, UserViewedProductNotification entity) {
    if (StringUtils.isBlank(entity.getRealmId())) {
      throw new BadRequestException("The realm ID is required.");
    }
    super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, UserViewedProductNotification entity) {
    throw new UnsupportedOperationException(
        "The UserViewedProductNotification table does not support update operations");
  }

  public List<UserViewedProductNotification> getByUsernameAndRealmId(String username, String realmId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsernameAndRealmId(tx, username, realmId);
    }
  }

  private List<UserViewedProductNotification> getByUsernameAndRealmId(
      TransactionContext tx,
      String username,
      String realmId)
  {
    username = User.normalizeUsername(username);
    String sQuery = "SELECT entity FROM UserViewedProductNotification entity" + //
        " WHERE entity.usernameLowercase=?1 AND entity.realmId=?2";
    return getList(tx, sQuery, username, realmId);
  }

  public void deleteByUsernameAndRealmId(TransactionContext tx, String username, String realmId) {
    getByUsernameAndRealmId(tx, username, realmId).forEach(entity -> delete(tx, entity));
  }

  public void deleteLegacyByUsername(TransactionContext tx, String username) {
    deleteByUsernameAndRealmId(tx, username, null /* realmId */);
  }

  private List<UserViewedProductNotification> getByRealmId(TransactionContext tx, String realmId) {
    String sQuery = "SELECT entity FROM UserViewedProductNotification entity" + //
        " WHERE entity.realmId=?1";
    return getList(tx, sQuery, realmId);
  }

  public void deleteByRealmId(TransactionContext tx, String realmId) {
    getByRealmId(tx, realmId).forEach(userViewedProductNotification -> delete(tx, userViewedProductNotification));
  }

  /**
   * Before Insight Brain 1.76, viewed notifications stored the username as the user entered it at login time,
   * not as it is stored in the authentication realm.
   * This means there may be multiple viewed notifications with the same notification ID and same case insensitive
   * username.
   * 
   * This method tries first to find a match by username case sensitive, then by username case insensitive.
   * In both cases, if there are multiple viewed notifications, then this method will return only one of them.
   */
  public UserViewedProductNotification getLegacyByUsernameAndNotificationId(
      TransactionContext tx,
      String username,
      String notificationId)
  {
    // Try to find a viewed notification that matches the username case sensitive.
    String sQuery = "SELECT entity FROM UserViewedProductNotification entity" + //
        " WHERE entity.username=?1 AND entity.realmId IS NULL AND entity.notificationId=?2";
    List<UserViewedProductNotification> userViewedProductNotifications = getList(tx, sQuery, username, notificationId);
    if (userViewedProductNotifications.isEmpty()) {
      // No viewed notification matches the username case sensitive. Try case-insensitive.
      username = User.normalizeUsername(username);
      sQuery = "SELECT entity FROM UserViewedProductNotification entity" + //
          " WHERE entity.usernameLowercase=?1 AND entity.realmId IS NULL AND entity.notificationId=?2";
      userViewedProductNotifications = getList(tx, sQuery, username, notificationId);
    }
    if (userViewedProductNotifications.isEmpty()) {
      return null;
    }
    return userViewedProductNotifications.get(0);
  }

  public List<UserViewedProductNotification> getLegacyByUsername(String username) {
    username = User.normalizeUsername(username);
    String sQuery = "SELECT entity FROM UserViewedProductNotification entity" + //
        " WHERE entity.usernameLowercase=?1 AND entity.realmId IS NULL";
    return getList(sQuery, username);
  }
}
