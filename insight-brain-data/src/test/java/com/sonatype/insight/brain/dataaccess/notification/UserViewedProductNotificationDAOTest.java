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
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserViewedProductNotificationDAOTest
    extends AbstractDbDAOTest
{
  private UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    userViewedProductNotificationDAO = daoFactory.createUserViewedProductNotificationDAO();
  }

  @Test
  public void testCRUD() {
    String notificationId = UUID.randomUUID().toString();
    String username = "tmpUser";
    String realmId = "testRealmId";

    // Create
    UserViewedProductNotification userViewedProductNotification =
        new UserViewedProductNotification(username, realmId, notificationId);
    userViewedProductNotificationDAO.insert(userViewedProductNotification);

    // Get
    assertUserViewedProductNotification(userViewedProductNotificationDAO.getById(userViewedProductNotification.getId()),
        userViewedProductNotification);

    assertThatThrownBy(() -> userViewedProductNotificationDAO.update(userViewedProductNotification))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("The UserViewedProductNotification table does not support update operations");

    // Delete
    userViewedProductNotificationDAO.delete(userViewedProductNotification);

    // Get
    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotification.getId())).isNull();
  }

  private void assertUserViewedProductNotification(
      UserViewedProductNotification actual,
      UserViewedProductNotification expected)
  {
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getNotificationId()).isEqualTo(expected.getNotificationId());
    assertThat(actual.getUsername()).isEqualTo(expected.getUsername());
    assertThat(actual.getUsernameLowercase()).isEqualTo(expected.getUsernameLowercase());
    assertThat(actual.getUsernameLowercase()).isEqualTo(User.normalizeUsername(expected.getUsername()));
    assertThat(actual.getRealmId()).isEqualTo(expected.getRealmId());
  }

  @Test
  public void testGetByUsernameAndRealmIdAndNotificationId() {
    String username = "testUser";
    String notificationId = "testNotificationId";
    UserViewedProductNotification userViewedProductNotification =
        tempEntity.newUserViewedProductNotification(username, User.INTERNAL_REALM_ID, notificationId);
    tempEntity.newUserViewedProductNotification("OtherUser", User.INTERNAL_REALM_ID, notificationId);
    tempEntity.newUserViewedProductNotification(username, "OtherRealmId", notificationId);
    tempEntity.newUserViewedProductNotification(username, User.INTERNAL_REALM_ID, "OtherNotificationId");

    UserViewedProductNotification retrieved = userViewedProductNotificationDAO
        .getByUsernameAndRealmIdAndNotificationId(username, User.INTERNAL_REALM_ID, notificationId);

    assertUserViewedProductNotification(retrieved, userViewedProductNotification);
  }

  @Test
  public void testGetByUsernameAndRealmIdAndNotificationId_UsernameCaseInsensitive() {
    String username = "testUser";
    String notificationId = "testNotificationId";
    UserViewedProductNotification userViewedProductNotification =
        tempEntity.newUserViewedProductNotification(username, User.INTERNAL_REALM_ID, notificationId);
    tempEntity.newUserViewedProductNotification("OtherUser", User.INTERNAL_REALM_ID, notificationId);
    tempEntity.newUserViewedProductNotification(username, "OtherRealmId", notificationId);
    tempEntity.newUserViewedProductNotification(username, User.INTERNAL_REALM_ID, "OtherNotificationId");

    UserViewedProductNotification retrieved = userViewedProductNotificationDAO
        .getByUsernameAndRealmIdAndNotificationId("TestUser", User.INTERNAL_REALM_ID, notificationId);

    assertUserViewedProductNotification(retrieved, userViewedProductNotification);
  }

  @Test
  public void testGetByUsernameAndRealmId() {
    String username = "testUser";
    String notificationId = "testNotificationId";
    UserViewedProductNotification userViewedProductNotification =
        tempEntity.newUserViewedProductNotification(username, User.INTERNAL_REALM_ID, notificationId);
    tempEntity.newUserViewedProductNotification("OtherUser", User.INTERNAL_REALM_ID, notificationId);
    tempEntity.newUserViewedProductNotification(username, "OtherRealmId", notificationId);

    List<UserViewedProductNotification> retrieved = userViewedProductNotificationDAO
        .getByUsernameAndRealmId(username, User.INTERNAL_REALM_ID);

    assertThat(retrieved).hasSize(1);
    assertUserViewedProductNotification(retrieved.get(0), userViewedProductNotification);
  }

  @Test
  public void testGetByUsernameAndRealmId_UsernameCaseInsensitive() {
    String username = "testUser";
    String notificationId = "testNotificationId";
    UserViewedProductNotification userViewedProductNotification =
        tempEntity.newUserViewedProductNotification(username, User.INTERNAL_REALM_ID, notificationId);
    tempEntity.newUserViewedProductNotification("OtherUser", User.INTERNAL_REALM_ID, notificationId);
    tempEntity.newUserViewedProductNotification(username, "OtherRealmId", notificationId);

    List<UserViewedProductNotification> retrieved =
        userViewedProductNotificationDAO.getByUsernameAndRealmId("TestUser", User.INTERNAL_REALM_ID);

    assertThat(retrieved).hasSize(1);
    assertUserViewedProductNotification(retrieved.get(0), userViewedProductNotification);
  }

  @Test
  public void testGetLegacyByUsernameAndNotificationId() {
    String username = "testUsername";
    String realmId = "testRealmId";
    String notificationId = "testNotificationId";
    tempEntity.newUserViewedProductNotification(username, realmId, notificationId);
    UserViewedProductNotification userViewedProductNotificationLegacy =
        tempEntity.newUserViewedProductNotificationLegacy(username, notificationId);

    try (TransactionContext tx = userViewedProductNotificationDAO.createTransactionContext()) {
      assertUserViewedProductNotification(
          userViewedProductNotificationDAO.getLegacyByUsernameAndNotificationId(tx, username, notificationId),
          userViewedProductNotificationLegacy);
    }
  }

  @Test
  public void testGetLegacyByUsernameAndNotificationId_UsernameCaseInsensitive() {
    String username = "testUsername";
    String realmId = "testRealmId";
    String notificationId = "testNotificationId";
    tempEntity.newUserViewedProductNotification(username, realmId, notificationId);
    UserViewedProductNotification userViewedProductNotificationLegacy =
        tempEntity.newUserViewedProductNotificationLegacy(username, notificationId);

    try (TransactionContext tx = userViewedProductNotificationDAO.createTransactionContext()) {
      assertUserViewedProductNotification(
          userViewedProductNotificationDAO.getLegacyByUsernameAndNotificationId(tx, "TestUsername", notificationId),
          userViewedProductNotificationLegacy);
    }
  }

  @Test
  public void testGetLegacyByUsername() {
    String username = "testUsername";
    String realmId = "testRealmId";
    String notificationId = "testNotificationId";
    tempEntity.newUserViewedProductNotification(username, realmId, notificationId);
    UserViewedProductNotification userViewedProductNotificationLegacy =
        tempEntity.newUserViewedProductNotificationLegacy(username, notificationId);

    List<UserViewedProductNotification> retrieved = userViewedProductNotificationDAO.getLegacyByUsername(username);
    assertThat(retrieved).hasSize(1);
    assertUserViewedProductNotification(retrieved.get(0), userViewedProductNotificationLegacy);
  }

  @Test
  public void testGetLegacyByUsername_UsernameCaseInsensitive() {
    String username = "testUsername";
    String realmId = "testRealmId";
    String notificationId = "testNotificationId";
    tempEntity.newUserViewedProductNotification(username, realmId, notificationId);
    UserViewedProductNotification userViewedProductNotificationLegacy =
        tempEntity.newUserViewedProductNotificationLegacy(username, notificationId);

    List<UserViewedProductNotification> retrieved =
        userViewedProductNotificationDAO.getLegacyByUsername("TestUsername");
    assertThat(retrieved).hasSize(1);
    assertUserViewedProductNotification(retrieved.get(0), userViewedProductNotificationLegacy);
  }

  @Test
  public void testInsert_RealmIdNull() {
    assertThatThrownBy(() -> tempEntity.newUserViewedProductNotification("testUsername", null /* realmId */,
        "testNotificationId")).isInstanceOf(BadRequestException.class).hasMessage("The realm ID is required.");
  }

  @Test
  public void testInsert_RealmIdWhitespace() {
    assertThatThrownBy(() -> tempEntity.newUserViewedProductNotification("testUsername", " " /* realmId */,
        "testNotificationId")).isInstanceOf(BadRequestException.class).hasMessage("The realm ID is required.");
  }
}
