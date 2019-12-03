/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.notification;

import java.util.List;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

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
    assertThat(notificationViewedList).hasSize(1);
    assertThat(notificationViewedList.get(0).getNotificationId()).isEqualTo(notificationId);
    assertThat(notificationViewedList.get(0).getUsername()).isEqualTo(username);

    assertThatThrownBy(() -> {
      userViewedNotificationMappingDAO.update(notificationViewed);
    }).isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("The UserViewedProductNotification table does not support update operations");

    // Delete
    userViewedNotificationMappingDAO.delete(notificationViewed);

    // Get
    notificationViewedList = userViewedNotificationMappingDAO.getByUsername(username);
    assertThat(notificationViewedList).isEmpty();
  }

  @Test
  public void testGetByUsernameAndNotificationId() {
    tempEntity.newUserViewedProductNotification("tmpUser1", UUID.randomUUID().toString());
    tempEntity.newUserViewedProductNotification("tmpUser2", UUID.randomUUID().toString());
    UserViewedProductNotification expected = tempEntity.newUserViewedProductNotification("tmpUser2", UUID.randomUUID()
        .toString());

    UserViewedProductNotification retrieved = userViewedNotificationMappingDAO.getByUsernameAndNotificationId(
        expected.getUsername(), expected.getNotificationId());

    assertThat(retrieved.getUsername()).isEqualTo(expected.getUsername());
    assertThat(retrieved.getNotificationId()).isEqualTo(expected.getNotificationId());
  }

  @Test
  public void testGetAll() {
    UserViewedProductNotification expected1 = tempEntity.newUserViewedProductNotification("tmpUser1", UUID.randomUUID()
        .toString());
    UserViewedProductNotification expected2 = tempEntity.newUserViewedProductNotification("tmpUser2", UUID.randomUUID()
        .toString());

    List<UserViewedProductNotification> notificationViewedList = userViewedNotificationMappingDAO.getAll();
    assertThat(notificationViewedList)
        .extracting(UserViewedProductNotification::getUsername, UserViewedProductNotification::getNotificationId)
        .containsExactlyInAnyOrder(tuple(expected1.getUsername(), expected1.getNotificationId()),
            tuple(expected2.getUsername(), expected2.getNotificationId()));
  }
}
