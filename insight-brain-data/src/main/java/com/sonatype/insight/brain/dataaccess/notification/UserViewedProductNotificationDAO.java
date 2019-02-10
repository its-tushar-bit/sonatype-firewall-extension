/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.notification;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.14.0
 */
public class UserViewedProductNotificationDAO
    extends AbstractOperationalSqlDAO<UserViewedProductNotification>
{
  public List<UserViewedProductNotification> getByUsername(final TransactionContext tx, final String username) {
    String sQuery = "SELECT entity FROM UserViewedProductNotification entity WHERE entity.username=?1";
    return getList(tx, sQuery, username);
  }

  public List<UserViewedProductNotification> getByUsername(final String username) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByUsername(tx, username);
    }
  }

  public UserViewedProductNotification getByUsernameAndNotificationId(final String username,
                                                                      final String notificationId)
  {
    String sQuery = "SELECT entity FROM UserViewedProductNotification entity WHERE entity.username=?1" + //
        " AND entity.notificationId=?2";

    return get(sQuery, username, notificationId);
  }

  public List<UserViewedProductNotification> getAll() {
    String sQuery = "SELECT entity FROM UserViewedProductNotification entity";
    return getList(sQuery);
  }

  @Override
  public void update(TransactionContext tx, UserViewedProductNotification entity) {
    throw new UnsupportedOperationException(
        "The UserViewedProductNotification table does not support update operations");
  }
}
