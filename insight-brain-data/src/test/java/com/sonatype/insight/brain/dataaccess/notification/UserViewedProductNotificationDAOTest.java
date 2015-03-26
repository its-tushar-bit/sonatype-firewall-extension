/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.notification;

import java.util.List;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class UserViewedProductNotificationDAOTest
    extends AbstractDbDAOTest
{
  private UserViewedProductNotificationDAO userViewedNotificationMappingDAO = new UserViewedProductNotificationDAO();

  @Test
  public void testCRUD() throws Exception {
    String notificationId = UUID.randomUUID().toString();
    String username = "tmpUser";

    // Create
    UserViewedProductNotification notificationViewed = new UserViewedProductNotification(username, notificationId);
    userViewedNotificationMappingDAO.insert(notificationViewed);

    // Get
    List<UserViewedProductNotification> notificationViewedList = userViewedNotificationMappingDAO
        .getByUsername(username);
    assertThat(notificationViewedList.size(), is(1));
    assertThat(notificationViewedList.get(0).getNotificationId(), is(notificationId));
    assertThat(notificationViewedList.get(0).getUsername(), is(username));

    try {
      userViewedNotificationMappingDAO.update(notificationViewed);
      fail("Expected UnsupportedOperationException");
    }
    catch (UnsupportedOperationException e) {
      assertThat(e.getMessage(), is("The UserViewedProductNotification table does not support update operations"));
    }

    // Delete
    userViewedNotificationMappingDAO.delete(notificationViewed);

    // Get
    notificationViewedList = userViewedNotificationMappingDAO.getByUsername(username);
    assertThat(notificationViewedList.size(), is(0));
  }
}
