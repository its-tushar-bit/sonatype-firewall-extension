/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.model.security.SamlGroup;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.SamlUserGroup;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.postgres.PostgresServer;

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

  private final SamlUserGroupDAO samlUserGroupDAO = new SamlUserGroupDAO();

  @Test
  public void testCRUD() {
    // Create
    SamlUser samlUser = createSamlUser();
    samlUserDAO.insert(samlUser);
    assertThat(samlUser.getId()).isNotNull();

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
  public void testGetByIds_Empty() {
    assertThat(samlUserDAO.getByIds(Collections.emptySet())).isEmpty();
  }

  @Test
  public void testGetByIds() {
    SamlUser samlUser1 = tempEntity.newSamlUser("userA", null, null, null, null);
    SamlUser samlUser2 = tempEntity.newSamlUser("userB", null, null, null, null);
    tempEntity.newSamlUser();

    assertThat(samlUserDAO.getByIds(
        new HashSet<>(Arrays.asList(samlUser1.getId(), samlUser2.getId()))))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlUser1, samlUser2);
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
  public void testDeleteCascadesToSamlUserGroups() {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();
    SamlGroup samlGroup1 = tempEntity.newSamlGroup();
    SamlGroup samlGroup2 = tempEntity.newSamlGroup();
    tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup1.getId());
    tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup2.getId());
    SamlUserGroup samlUserGroup21 = tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup1.getId());
    SamlUserGroup samlUserGroup22 = tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup2.getId());

    samlUserDAO.delete(samlUser1);

    assertThat(samlUserDAO.getById(samlUser1.getId())).isNull();
    assertThat(samlUserGroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(samlUserGroup21, samlUserGroup22);
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

  @Test
  public void testGetByUsernames_Empty() {
    assertThat(samlUserDAO.getByUsernames(Collections.emptySet())).isEmpty();
  }

  @Test
  public void testGetByUsernames() {
    SamlUser samlUser1 = tempEntity.newSamlUser("userA", null, null, null, null);
    SamlUser samlUser2 = tempEntity.newSamlUser("userB", null, null, null, null);
    tempEntity.newSamlUser();

    Set<String> usernames = new HashSet<>(Arrays.asList(samlUser1.getUsername(), samlUser2.getUsername()));
    assertThat(samlUserDAO.getByUsernames(usernames))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlUser1, samlUser2);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_ExactName() {
    SamlUser samlUser = tempEntity.newSamlUser("userA", "bob", "smith", null, null);
    tempEntity.newSamlUser("other", "john", "smith", null, null);

    assertThat(samlUserDAO.findUsersByNameOrUsernameQuery("BoB sMiTh")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlUser);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_PrefixName() {
    SamlUser samlUser1 = tempEntity.newSamlUser("userA", "BOB", "smith", null, null);
    SamlUser samlUser2 = tempEntity.newSamlUser("userB", "bob", "doe", null, null);
    tempEntity.newSamlUser("other", "john", "smith", null, null);

    assertThat(samlUserDAO.findUsersByNameOrUsernameQuery("BoB%")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlUser1, samlUser2);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_SuffixName() {
    SamlUser samlUser1 = tempEntity.newSamlUser("userA", "bob", "SMITH", null, null);
    SamlUser samlUser2 = tempEntity.newSamlUser("userB", "john", "smith", null, null);
    tempEntity.newSamlUser("other", "john", "doe", null, null);

    assertThat(samlUserDAO.findUsersByNameOrUsernameQuery("%SmItH")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlUser1, samlUser2);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_PrefixAndSuffixName() {
    SamlUser samlUser1 = tempEntity.newSamlUser("userA", "johnny", "smith", null, null);
    SamlUser samlUser2 = tempEntity.newSamlUser("userB", "bobby", "smithson", null, null);
    tempEntity.newSamlUser("other", "john", "doe", null, null);

    assertThat(samlUserDAO.findUsersByNameOrUsernameQuery("%y SmItH%")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlUser1, samlUser2);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_LastNameNull() {
    SamlUser samlUser1 = tempEntity.newSamlUser("userA", "johnny smith", null, null, null);
    SamlUser samlUser2 = tempEntity.newSamlUser("userB", "bobby smithson", null, null, null);
    tempEntity.newSamlUser("other", "john", "doe", null, null);

    assertThat(samlUserDAO.findUsersByNameOrUsernameQuery("%SmItH%")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlUser1, samlUser2);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_ExactUserName() {
    SamlUser samlUser1 = tempEntity.newSamlUser("userA", "johnny smith", null, null, null);
    tempEntity.newSamlUser("userB", "bobby smithson", null, null, null);

    assertThat(samlUserDAO.findUsersByNameOrUsernameQuery("userA")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlUser1);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_PrefixUserName() {
    SamlUser samlUser1 = tempEntity.newSamlUser("userA", "BOB", "smith", null, null);
    SamlUser samlUser2 = tempEntity.newSamlUser("userB", "bob", "doe", null, null);
    tempEntity.newSamlUser("other", "john", "smith", null, null);

    assertThat(samlUserDAO.findUsersByNameOrUsernameQuery("user%")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlUser1, samlUser2);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_SuffixUserName() {
    SamlUser samlUser1 = tempEntity.newSamlUser("userA-sonatype", "bob", "SMITH", null, null);
    SamlUser samlUser2 = tempEntity.newSamlUser("userB-sonatype", "john", "smith", null, null);
    tempEntity.newSamlUser("userC-sonatype-1", "john", "doe", null, null);

    assertThat(samlUserDAO.findUsersByNameOrUsernameQuery("%-SoNaTypE")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlUser1, samlUser2);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_PrefixAndSuffixUserName() {
    SamlUser samlUser1 = tempEntity.newSamlUser("userA-sonatype-1", "johnny", "smith", null, null);
    SamlUser samlUser2 = tempEntity.newSamlUser("userB-sonatype-2", "bobby", "smithson", null, null);
    tempEntity.newSamlUser("other", "john", "doe", null, null);

    assertThat(samlUserDAO.findUsersByNameOrUsernameQuery("%-SoNaTypE%")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlUser1, samlUser2);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_PrefixAndSuffixUserName_postgres() {
    testInPostgres(() -> {
      SamlUser samlUser1 = tempEntity.newSamlUser("userA-postgres-1", "johnny", "smith", null, null);
      SamlUser samlUser2 = tempEntity.newSamlUser("userB-postgres-2", "bobby", "smithson", null, null);
      tempEntity.newSamlUser("other", "john", "doe", null, null);

      assertThat(samlUserDAO.findUsersByNameOrUsernameQuery("%-PoStgREs%"))
          .usingRecursiveFieldByFieldElementComparator()
          .containsExactly(samlUser1, samlUser2);
    });
  }

  private void testInPostgres(Runnable test) {
    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      test.run();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  private SamlUser createSamlUser() {
    return new SamlUser("someUsername", "someFirstName", "someLastName", "someEmail@someDomain.com",
        new LinkedHashSet<>(Arrays.asList("someGroup1", "someGroup2")));
  }
}
