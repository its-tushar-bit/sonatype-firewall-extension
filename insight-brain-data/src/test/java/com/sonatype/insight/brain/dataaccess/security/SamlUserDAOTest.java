/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SamlUserDAOTest
    extends AbstractDbDAOTest
{
  private final SamlUserDAO samlUserDAO = new SamlUserDAO();

  private final UserTokenDAO userTokenDAO = new UserTokenDAO();

  private final DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();

  private final UserFilterDAO userFilterDAO = new UserFilterDAO();

  private final UserViewedProductNotificationDAO userViewedProductNotificationDAO =
      new UserViewedProductNotificationDAO();

  @Test
  public void testCRUD() {
    // Create
    SamlUser samlUser = createSamlUser();
    samlUserDAO.insert(samlUser);
    assertThat(samlUser.getId()).isNotNull();
    tempEntity.register(samlUser);

    // Read
    SamlUser storedSamlUser = samlUserDAO.getById(samlUser.getId());
    assertThat(storedSamlUser).isNotNull();
    assertThat(storedSamlUser).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(samlUser);

    // Update
    samlUser.setUsername(samlUser.getUsername() + "2");
    samlUser.setFirstName(samlUser.getFirstName() + "2");
    samlUser.setLastName(samlUser.getLastName() + "2");
    samlUser.setEmail(samlUser.getEmail() + "2");
    samlUser.setGroups(new LinkedHashSet<>(Arrays.asList("someGroup3", "someGroup4")));
    samlUserDAO.update(samlUser);
    storedSamlUser = samlUserDAO.getById(samlUser.getId());
    assertThat(storedSamlUser).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(samlUser);

    // Delete
    samlUserDAO.delete(samlUser);
    assertThat(samlUserDAO.getById(samlUser.getId())).isNull();
  }

  @Test
  public void testGetByUsername() {
    SamlUser samlUser = tempEntity.newSamlUser();
    tempEntity.newSamlUser();

    assertThat(samlUserDAO.getByUsername(samlUser.getUsername())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(samlUser);
  }

  @Test
  public void testUpsertByUsername_Insert() {
    SamlUser samlUser = createSamlUser();

    samlUserDAO.upsertByUsername(samlUser);

    assertThat(samlUserDAO.getByUsername(samlUser.getUsername())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(samlUser);
  }

  @Test
  public void testUpsertByUsername_Update() {
    SamlUser samlUser = tempEntity.newSamlUser();
    samlUser.setFirstName(samlUser.getFirstName() + "2");
    samlUser.setLastName(samlUser.getLastName() + "2");
    samlUser.setEmail(samlUser.getEmail() + "2");
    samlUser.setGroups(new LinkedHashSet<>(Arrays.asList("someGroup3", "someGroup4")));

    samlUserDAO.upsertByUsername(samlUser);

    assertThat(samlUserDAO.getByUsername(samlUser.getUsername())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(samlUser);
  }

  @Test
  public void testDeleteCascadesToUserToken() {
    SamlUser samlUser = tempEntity.newSamlUser();
    UserToken userToken1 =
        tempEntity.newUserToken(samlUser.getUsername(), "userCode1", "passCode1", SamlUser.SAML_REALM_ID);
    UserToken userToken2 = tempEntity.newUserToken("other", "userCode2", "passCode2", SamlUser.SAML_REALM_ID);
    UserToken userToken3 =
        tempEntity.newUserToken(samlUser.getUsername(), "userCode3", "passCode3", User.INTERNAL_REALM_ID);

    samlUserDAO.delete(samlUser);

    assertThat(samlUserDAO.getById(samlUser.getId())).isNull();
    assertThat(userTokenDAO.getById(userToken1.getId())).isNull();
    assertThat(userTokenDAO.getById(userToken2.getId())).isNotNull();
    assertThat(userTokenDAO.getById(userToken3.getId())).isNotNull();
  }

  @Test
  public void testDeleteCascadesToDashboardFilters() {
    SamlUser samlUser = tempEntity.newSamlUser();
    DashboardFilter dashboardFilter1 =
        tempEntity.newDashboardFilter(samlUser.getUsername(), SamlUser.SAML_REALM_ID, "filterName1", "filter1");
    DashboardFilter dashboardFilter2 =
        tempEntity.newDashboardFilter(samlUser.getUsername(), SamlUser.SAML_REALM_ID, "filterName2", "filter2");
    DashboardFilter dashboardFilter3 =
        tempEntity.newDashboardFilter("other", SamlUser.SAML_REALM_ID, "filterName3", "filter3");
    DashboardFilter dashboardFilter4 =
        tempEntity.newDashboardFilter(samlUser.getUsername(), "other", "filterName4", "filter4");
    DashboardFilter dashboardFilter5 =
        tempEntity.newDashboardFilter("other", "other", "filterName5", "filter5");

    samlUserDAO.delete(samlUser);

    assertThat(samlUserDAO.getById(samlUser.getId())).isNull();
    assertThat(dashboardFilterDAO.getById(dashboardFilter1.getId())).isNull();
    assertThat(dashboardFilterDAO.getById(dashboardFilter2.getId())).isNull();
    assertThat(dashboardFilterDAO.getById(dashboardFilter3.getId())).isNotNull();
    assertThat(dashboardFilterDAO.getById(dashboardFilter4.getId())).isNotNull();
    assertThat(dashboardFilterDAO.getById(dashboardFilter5.getId())).isNotNull();
  }

  @Test
  public void testDeleteCascadesToUserFilters() {
    SamlUser samlUser = tempEntity.newSamlUser();
    UserFilter userFilter1 = tempEntity.newUserFilter(samlUser.getUsername(), SamlUser.SAML_REALM_ID, "filterName1",
        UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "filter1");
    UserFilter userFilter2 = tempEntity.newUserFilter(samlUser.getUsername(), SamlUser.SAML_REALM_ID, "filterName2",
        UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "filter2");
    UserFilter userFilter3 = tempEntity.newUserFilter("other", SamlUser.SAML_REALM_ID, "filterName3",
        UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "filter3");
    UserFilter userFilter4 = tempEntity.newUserFilter(samlUser.getUsername(), "other", "filterName4",
        UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "filter4");
    UserFilter userFilter5 = tempEntity.newUserFilter("other", "other", "filterName5",
        UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "filter5");

    samlUserDAO.delete(samlUser);

    assertThat(samlUserDAO.getById(samlUser.getId())).isNull();
    assertThat(userFilterDAO.getById(userFilter1.getId())).isNull();
    assertThat(userFilterDAO.getById(userFilter2.getId())).isNull();
    assertThat(userFilterDAO.getById(userFilter3.getId())).isNotNull();
    assertThat(userFilterDAO.getById(userFilter4.getId())).isNotNull();
    assertThat(userFilterDAO.getById(userFilter5.getId())).isNotNull();
  }

  @Test
  public void testDeleteCascadesToUserViewedProductNotifications() {
    SamlUser samlUser = tempEntity.newSamlUser();
    UserViewedProductNotification userViewedProductNotification1 =
        tempEntity.newUserViewedProductNotification(samlUser.getUsername(), SamlUser.SAML_REALM_ID, "notificationId1");
    UserViewedProductNotification userViewedProductNotification2 =
        tempEntity.newUserViewedProductNotification(samlUser.getUsername(), SamlUser.SAML_REALM_ID, "notificationId2");
    UserViewedProductNotification userViewedProductNotification3 =
        tempEntity.newUserViewedProductNotification("other", SamlUser.SAML_REALM_ID, "notificationId3");
    UserViewedProductNotification userViewedProductNotification4 =
        tempEntity.newUserViewedProductNotification(samlUser.getUsername(), "other", "notificationId4");
    UserViewedProductNotification userViewedProductNotification5 =
        tempEntity.newUserViewedProductNotification("other", "other", "notificationId5");

    samlUserDAO.delete(samlUser);

    assertThat(samlUserDAO.getById(samlUser.getId())).isNull();
    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotification1.getId())).isNull();
    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotification2.getId())).isNull();
    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotification3.getId())).isNotNull();
    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotification4.getId())).isNotNull();
    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotification5.getId())).isNotNull();
  }

  @Test
  public void testGetAll() {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();

    List<SamlUser> users = samlUserDAO.getAll();
    assertThat(users).hasSize(2);
    assertSamlUser(samlUser1, users);
    assertSamlUser(samlUser2, users);
  }

  private void assertSamlUser(SamlUser expectedSamlUser, List<SamlUser> users) {
    SamlUser foundUser = users.stream()
        .filter(samlUser -> expectedSamlUser.getUsername().equals(samlUser.getUsername())).findFirst().orElse(null);
    assertThat(foundUser).isNotNull().usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedSamlUser);
  }

  @Test
  public void testGetByUsernameNotNull_Exists() {
    SamlUser samlUser = tempEntity.newSamlUser();

    assertThat(samlUserDAO.getByUsernameNotNull(samlUser.getUsername())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(samlUser);
  }

  @Test
  public void testGetByUsernameNotNull_DoesNotExist() {
    String username = "doesNotExist";

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> samlUserDAO.getByUsernameNotNull(username))
        .withMessageContaining("Cannot find a SAML user with username " + username + ".");
  }

  private SamlUser createSamlUser() {
    return new SamlUser("someUsername", "someFirstName", "someLastName", "someEmail@someDomain.com",
        new LinkedHashSet<>(Arrays.asList("someGroup1", "someGroup2")));
  }
}
