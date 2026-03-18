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
import java.util.UUID;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.model.security.OAuth2Group;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.OAuth2UserGroup;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class OAuth2UserDAOTest
    extends AbstractDbDAOTest
{
  private OAuth2UserDAO oAuth2UserDAO;

  private UserTokenDAO userTokenDAO;

  private DashboardFilterDAO dashboardFilterDAO;

  private UserFilterDAO userFilterDAO;

  private UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  private OAuth2UserGroupDAO oAuth2UserGroupDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    oAuth2UserDAO = daoFactory.createOAuth2UserDAO();
    userTokenDAO = daoFactory.createUserTokenDAO();
    dashboardFilterDAO = daoFactory.createDashboardFilterDAO();
    userFilterDAO = daoFactory.createUserFilterDAO();
    userViewedProductNotificationDAO = daoFactory.createUserViewedProductNotificationDAO();
    oAuth2UserGroupDAO = daoFactory.createOAuth2UserGroupDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    OAuth2User oAuth2User = createOauth2User();
    oAuth2UserDAO.insert(oAuth2User);
    assertThat(oAuth2User.getId()).isNotNull();

    // Read
    OAuth2User storedOAuth2User = oAuth2UserDAO.getById(oAuth2User.getId());
    assertThat(storedOAuth2User).isNotNull();
    assertThat(storedOAuth2User).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(oAuth2User);

    // Update
    oAuth2User.setUsername(oAuth2User.getUsername() + "2");
    oAuth2User.setFirstName(oAuth2User.getFirstName() + "2");
    oAuth2User.setLastName(oAuth2User.getLastName() + "2");
    oAuth2User.setEmail(oAuth2User.getEmail() + "2");
    oAuth2User.setGroups(new LinkedHashSet<>(Arrays.asList("someGroup3", "someGroup4")));
    oAuth2UserDAO.update(oAuth2User);
    storedOAuth2User = oAuth2UserDAO.getById(oAuth2User.getId());
    assertThat(storedOAuth2User).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(oAuth2User);

    // Delete
    oAuth2UserDAO.delete(oAuth2User);
    assertThat(oAuth2UserDAO.getById(oAuth2User.getId())).isNull();
  }

  @Test
  public void testGetByIds_Empty() {
    assertThat(oAuth2UserDAO.getByIds(Collections.emptySet())).isEmpty();
  }

  @Test
  public void testGetByIds() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA", null, null, null, null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("userB", null, null, null, null);
    tempEntity.newOAuth2User();

    assertThat(oAuth2UserDAO.getByIds(
        new HashSet<>(Arrays.asList(oAuth2User1.getId(), oAuth2User2.getId()))))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(oAuth2User1, oAuth2User2);
  }

  @Test
  public void testGetByUsername() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User();
    tempEntity.newOAuth2User();

    assertThat(oAuth2UserDAO.getByUsername(oAuth2User.getUsername())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(oAuth2User);
  }

  @Test
  public void testUpsertByUsername_Insert() {
    OAuth2User oAuth2User = createOauth2User();

    oAuth2UserDAO.upsertByUsername(oAuth2User);

    assertThat(oAuth2UserDAO.getByUsername(oAuth2User.getUsername())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(oAuth2User);
  }

  @Test
  public void testUpsertByUsername_Update() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User();
    oAuth2User.setFirstName(oAuth2User.getFirstName() + "2");
    oAuth2User.setLastName(oAuth2User.getLastName() + "2");
    oAuth2User.setEmail(oAuth2User.getEmail() + "2");
    oAuth2User.setGroups(new LinkedHashSet<>(Arrays.asList("someGroup3", "someGroup4")));

    oAuth2UserDAO.upsertByUsername(oAuth2User);

    assertThat(oAuth2UserDAO.getByUsername(oAuth2User.getUsername())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(oAuth2User);
  }

  @Test
  public void testDeleteCascadesToUserToken() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User();
    UserToken userToken1 =
        tempEntity.newUserToken(oAuth2User.getUsername(), "userCode1", "passCode1", OAuth2User.OAUTH2_REALM_ID);
    UserToken userToken2 = tempEntity.newUserToken("other", "userCode2", "passCode2", OAuth2User.OAUTH2_REALM_ID);
    UserToken userToken3 =
        tempEntity.newUserToken(oAuth2User.getUsername(), "userCode3", "passCode3", User.INTERNAL_REALM_ID);

    oAuth2UserDAO.delete(oAuth2User);

    assertThat(oAuth2UserDAO.getById(oAuth2User.getId())).isNull();
    assertThat(userTokenDAO.getById(userToken1.getId())).isNull();
    assertThat(userTokenDAO.getById(userToken2.getId())).isNotNull();
    assertThat(userTokenDAO.getById(userToken3.getId())).isNotNull();
  }

  @Test
  public void testDeleteCascadesToDashboardFilters() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User();
    DashboardFilter dashboardFilter1 =
        tempEntity.newDashboardFilter(oAuth2User.getUsername(), OAuth2User.OAUTH2_REALM_ID, "filterName1", "filter1");
    DashboardFilter dashboardFilter2 =
        tempEntity.newDashboardFilter(oAuth2User.getUsername(), OAuth2User.OAUTH2_REALM_ID, "filterName2", "filter2");
    DashboardFilter dashboardFilter3 =
        tempEntity.newDashboardFilter("other", OAuth2User.OAUTH2_REALM_ID, "filterName3", "filter3");
    DashboardFilter dashboardFilter4 =
        tempEntity.newDashboardFilter(oAuth2User.getUsername(), "other", "filterName4", "filter4");
    DashboardFilter dashboardFilter5 =
        tempEntity.newDashboardFilter("other", "other", "filterName5", "filter5");

    oAuth2UserDAO.delete(oAuth2User);

    assertThat(oAuth2UserDAO.getById(oAuth2User.getId())).isNull();
    assertThat(dashboardFilterDAO.getById(dashboardFilter1.getId())).isNull();
    assertThat(dashboardFilterDAO.getById(dashboardFilter2.getId())).isNull();
    assertThat(dashboardFilterDAO.getById(dashboardFilter3.getId())).isNotNull();
    assertThat(dashboardFilterDAO.getById(dashboardFilter4.getId())).isNotNull();
    assertThat(dashboardFilterDAO.getById(dashboardFilter5.getId())).isNotNull();
  }

  @Test
  public void testDeleteCascadesToUserFilters() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User();
    UserFilter userFilter1 =
        tempEntity.newUserFilter(oAuth2User.getUsername(), OAuth2User.OAUTH2_REALM_ID, "filterName1",
            UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "filter1");
    UserFilter userFilter2 =
        tempEntity.newUserFilter(oAuth2User.getUsername(), OAuth2User.OAUTH2_REALM_ID, "filterName2",
            UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "filter2");
    UserFilter userFilter3 = tempEntity.newUserFilter("other", OAuth2User.OAUTH2_REALM_ID, "filterName3",
        UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "filter3");
    UserFilter userFilter4 = tempEntity.newUserFilter(oAuth2User.getUsername(), "other", "filterName4",
        UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "filter4");
    UserFilter userFilter5 = tempEntity.newUserFilter("other", "other", "filterName5",
        UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "filter5");

    oAuth2UserDAO.delete(oAuth2User);

    assertThat(oAuth2UserDAO.getById(oAuth2User.getId())).isNull();
    assertThat(userFilterDAO.getById(userFilter1.getId())).isNull();
    assertThat(userFilterDAO.getById(userFilter2.getId())).isNull();
    assertThat(userFilterDAO.getById(userFilter3.getId())).isNotNull();
    assertThat(userFilterDAO.getById(userFilter4.getId())).isNotNull();
    assertThat(userFilterDAO.getById(userFilter5.getId())).isNotNull();
  }

  @Test
  public void testDeleteCascadesToUserViewedProductNotifications() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User();
    UserViewedProductNotification userViewedProductNotification1 =
        tempEntity.newUserViewedProductNotification(oAuth2User.getUsername(), OAuth2User.OAUTH2_REALM_ID,
            "notificationId1");
    UserViewedProductNotification userViewedProductNotification2 =
        tempEntity.newUserViewedProductNotification(oAuth2User.getUsername(), OAuth2User.OAUTH2_REALM_ID,
            "notificationId2");
    UserViewedProductNotification userViewedProductNotification3 =
        tempEntity.newUserViewedProductNotification("other", OAuth2User.OAUTH2_REALM_ID, "notificationId3");
    UserViewedProductNotification userViewedProductNotification4 =
        tempEntity.newUserViewedProductNotification(oAuth2User.getUsername(), "other", "notificationId4");
    UserViewedProductNotification userViewedProductNotification5 =
        tempEntity.newUserViewedProductNotification("other", "other", "notificationId5");

    oAuth2UserDAO.delete(oAuth2User);

    assertThat(oAuth2UserDAO.getById(oAuth2User.getId())).isNull();
    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotification1.getId())).isNull();
    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotification2.getId())).isNull();
    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotification3.getId())).isNotNull();
    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotification4.getId())).isNotNull();
    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotification5.getId())).isNotNull();
  }

  @Test
  public void testDeleteCascadesToOauth2UserGroups() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User();
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User();
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group();
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group();
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group1.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group2.getId());
    OAuth2UserGroup oAuth2UserGroup21 = tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group1.getId());
    OAuth2UserGroup oAuth2UserGroup22 = tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group2.getId());

    oAuth2UserDAO.delete(oAuth2User1);

    assertThat(oAuth2UserDAO.getById(oAuth2User1.getId())).isNull();
    assertThat(oAuth2UserGroupDAO.getAll()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(oAuth2UserGroup21, oAuth2UserGroup22);
  }

  @Test
  public void testGetAll() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User();
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User();

    List<OAuth2User> users = oAuth2UserDAO.getAll();
    assertThat(users).hasSize(2);
    assertOauth2User(oAuth2User1, users);
    assertOauth2User(oAuth2User2, users);
  }

  private void assertOauth2User(OAuth2User expectedOAuth2User, List<OAuth2User> users) {
    OAuth2User foundUser = users.stream()
        .filter(oAuth2User -> expectedOAuth2User.getUsername().equals(oAuth2User.getUsername()))
        .findFirst()
        .orElse(null);
    assertThat(foundUser).isNotNull()
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(expectedOAuth2User);
  }

  @Test
  public void testGetByUsernameNotNull_Exists() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User();

    assertThat(oAuth2UserDAO.getByUsernameNotNull(oAuth2User.getUsername())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(oAuth2User);
  }

  @Test
  public void testGetByUsernameNotNull_DoesNotExist() {
    String username = "doesNotExist";

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> oAuth2UserDAO.getByUsernameNotNull(username))
        .withMessageContaining("Cannot find a OAuth2 user with username " + username + ".");
  }

  @Test
  public void testGetByUsernames_Empty() {
    assertThat(oAuth2UserDAO.getByUsernames(Collections.emptySet())).isEmpty();
  }

  @Test
  public void testGetByUsernames() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA", null, null, null, null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("userB", null, null, null, null);
    tempEntity.newOAuth2User();

    Set<String> usernames = new HashSet<>(Arrays.asList(oAuth2User1.getUsername(), oAuth2User2.getUsername()));
    assertThat(oAuth2UserDAO.getByUsernames(usernames))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2User1, oAuth2User2);
  }

  @Test
  public void testGetByEmails_Empty() {
    assertThat(oAuth2UserDAO.getByEmails(Collections.emptySet())).isEmpty();
  }

  @Test
  public void testGetByEmails() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA", null, null, "usera@sonatype.com", null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("userB", null, null, "userb@sonatype.com", null);
    tempEntity.newOAuth2User();

    Set<String> emails = Set.of(oAuth2User1.getEmail(), oAuth2User2.getEmail());
    assertThat(oAuth2UserDAO.getByEmails(emails)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2User1, oAuth2User2);
  }

  @Test
  public void testGetByRealNames_Empty() {
    assertThat(oAuth2UserDAO.getByRealNames(Collections.emptySet())).isEmpty();
  }

  @Test
  public void testGetByRealNames() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA", "Mark", "Mywords", null, null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("userB", "Justin", "Time", null, null);
    tempEntity.newOAuth2User();

    Set<String> realNames = Set.of("Mark Mywords", "Justin Time");
    assertThat(oAuth2UserDAO.getByRealNames(realNames)).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2User1, oAuth2User2);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_ExactName() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User("userA", "bob", "smith", null, null);
    tempEntity.newOAuth2User("other", "john", "smith", null, null);

    assertThat(oAuth2UserDAO.findUsersByNameOrUsernameQuery("BoB sMiTh")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2User);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_PrefixName() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA", "BOB", "smith", null, null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("userB", "bob", "doe", null, null);
    tempEntity.newOAuth2User("other", "john", "smith", null, null);

    assertThat(oAuth2UserDAO.findUsersByNameOrUsernameQuery("BoB%")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2User1, oAuth2User2);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_SuffixName() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA", "bob", "SMITH", null, null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("userB", "john", "smith", null, null);
    tempEntity.newOAuth2User("other", "john", "doe", null, null);

    assertThat(oAuth2UserDAO.findUsersByNameOrUsernameQuery("%SmItH")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2User1, oAuth2User2);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_PrefixAndSuffixName() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA", "johnny", "smith", null, null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("userB", "bobby", "smithson", null, null);
    tempEntity.newOAuth2User("other", "john", "doe", null, null);

    assertThat(oAuth2UserDAO.findUsersByNameOrUsernameQuery("%y SmItH%")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2User1, oAuth2User2);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_LastNameNull() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA", "johnny smith", null, null, null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("userB", "bobby smithson", null, null, null);
    tempEntity.newOAuth2User("other", "john", "doe", null, null);

    assertThat(oAuth2UserDAO.findUsersByNameOrUsernameQuery("%SmItH%")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2User1, oAuth2User2);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_ExactUserName() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA", "johnny smith", null, null, null);
    tempEntity.newOAuth2User("userB", "bobby smithson", null, null, null);

    assertThat(oAuth2UserDAO.findUsersByNameOrUsernameQuery("userA")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2User1);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_PrefixUserName() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA", "BOB", "smith", null, null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("userB", "bob", "doe", null, null);
    tempEntity.newOAuth2User("other", "john", "smith", null, null);

    assertThat(oAuth2UserDAO.findUsersByNameOrUsernameQuery("user%")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2User1, oAuth2User2);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_SuffixUserName() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA-sonatype", "bob", "SMITH", null, null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("userB-sonatype", "john", "smith", null, null);
    tempEntity.newOAuth2User("userC-sonatype-1", "john", "doe", null, null);

    assertThat(oAuth2UserDAO.findUsersByNameOrUsernameQuery("%-SoNaTypE")).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2User1, oAuth2User2);
  }

  @Test
  public void testFindUsersByNameOrUsernameQuery_PrefixAndSuffixUserName() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA-sonatype-1", "johnny", "smith", null, null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("userB-sonatype-2", "bobby", "smithson", null, null);
    tempEntity.newOAuth2User("other", "john", "doe", null, null);

    assertThat(
        oAuth2UserDAO.findUsersByNameOrUsernameQuery("%-SoNaTypE%")).usingRecursiveFieldByFieldElementComparator()
            .containsExactly(oAuth2User1, oAuth2User2);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testFindUsersByNameOrUsernameQuery_PrefixAndSuffixUserName_postgres() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA-postgres-1", "johnny", "smith", null, null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("userB-postgres-2", "bobby", "smithson", null, null);
    tempEntity.newOAuth2User("other", "john", "doe", null, null);

    assertThat(oAuth2UserDAO.findUsersByNameOrUsernameQuery("%-PoStgREs%"))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(oAuth2User1, oAuth2User2);
  }

  @Test
  public void testWithAllUsersWithGroups() {
    String uuid = UUID.randomUUID().toString();
    String group1 = "group1" + uuid;
    String group2 = "group2" + uuid;
    String group3 = "group3" + uuid;

    Set<String> user1Groups = new HashSet<>(Arrays.asList(group1, group2));
    Set<String> user2Groups = new HashSet<>(Arrays.asList(group1, group2, group3));
    Set<String> user3Groups = new HashSet<>(Arrays.asList(group1));
    Set<String> user4Groups = new HashSet<>();

    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("username1" + uuid, user1Groups);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("username2" + uuid, user2Groups);
    OAuth2User oAuth2User3 = tempEntity.newOAuth2User("username3" + uuid, user3Groups);
    OAuth2User oAuth2User4 = tempEntity.newOAuth2User("username4" + uuid, user4Groups);
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group(group1);
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group(group2);
    OAuth2Group oAuth2Group3 = tempEntity.newOAuth2Group(group3);
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group1.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group2.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group1.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group2.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group3.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User3.getId(), oAuth2Group1.getId());

    oAuth2UserDAO.withAllUsersWithGroups((OAuth2User user) -> {
      if (user.getId().equals(oAuth2User1.getId())) {
        assertOAuth2UserWithGroups(oAuth2User1, user, user1Groups);
        return;
      }

      if (user.getId().equals(oAuth2User2.getId())) {
        assertOAuth2UserWithGroups(oAuth2User2, user, user2Groups);
        return;
      }

      if (user.getId().equals(oAuth2User3.getId())) {
        assertOAuth2UserWithGroups(oAuth2User3, user, user3Groups);
        return;
      }

      if (user.getId().equals(oAuth2User4.getId())) {
        assertOAuth2UserWithGroups(oAuth2User4, user, user4Groups);
        return;
      }

      // Should never reach this point
      throw new RuntimeException(String.format("Unexpected user with id: %s", user.getId()));
    });
  }

  private void assertOAuth2UserWithGroups(
      OAuth2User expectedSamlUser,
      OAuth2User foundUser,
      Set<String> expectedGroups)
  {
    assertThat(foundUser).isNotNull()
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringFields("groupsJson")
        .isEqualTo(expectedSamlUser);

    assertThat(foundUser.getGroups()).containsAll(expectedGroups);
  }

  private OAuth2User createOauth2User() {
    return new OAuth2User("someUsername", "someFirstName", "someLastName", "someEmail@someDomain.com",
        new LinkedHashSet<>(Arrays.asList("someGroup1", "someGroup2")));
  }
}
