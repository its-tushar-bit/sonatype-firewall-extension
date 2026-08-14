/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.ide.UserIdePolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.filter.UserFilter;
import com.sonatype.insight.brain.model.filter.UserFilterType;
import com.sonatype.insight.brain.model.notification.UserViewedProductNotification;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserToken;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @since 1.7
 */
public class UserDAOTest
    extends AbstractDbDAOTest
{
  private DashboardFilterDAO dashboardFilterDAO;

  private UserDAO userDAO;

  private UserFilterDAO userFilterDAO;

  private UserViewedProductNotificationDAO userViewedProductNotificationDAO;

  private UserIdePolicyEvaluationDAO userIdePolicyEvaluationDAO;

  private RoleDAO roleDAO;

  private MembershipMappingDAO membershipMappingDAO;

  private UserTokenDAO userTokenDAO;

  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dashboardFilterDAO = daoFactory.createDashboardFilterDAO();
    userDAO = daoFactory.createUserDAO();
    userFilterDAO = daoFactory.createUserFilterDAO();
    userViewedProductNotificationDAO = daoFactory.createUserViewedProductNotificationDAO();
    userIdePolicyEvaluationDAO = daoFactory.createUserIdePolicyEvaluationDAO();
    roleDAO = daoFactory.createRoleDAO();
    membershipMappingDAO = daoFactory.createMembershipMappingDAO();
    userTokenDAO = daoFactory.createUserTokenDAO();
    systemConfigurationPropertyDAO = daoFactory.createSystemConfigurationPropertyDAO();
  }

  @Test
  public void testCRUD() {
    String username = "testCRUD";
    // Insert
    User user = createUser(username);
    String userId = user.getId();
    assertThat(userId).isNotNull();

    // Get
    user = userDAO.getByIdNotNull(userId);
    assertThat(user.getUsername()).isEqualTo(username);

    // Update
    username += "Updated";
    user.setUsername(username);
    userDAO.update(user);

    // Get
    user = userDAO.getByIdNotNull(userId);
    assertThat(user.getUsername()).isEqualTo(username);

    // Delete
    userDAO.delete(user);

    // Get
    user = userDAO.getById(userId);
    assertThat(user).isNull();
  }

  @Test
  public void testGetByUsernames() {
    User testUser1 = createUser("testUser1");
    User testUser2 = createUser("testUser2");
    List<User> users = userDAO.getByUsernames(Sets.newHashSet("testUser1", "testUser2"));
    // getByUsernames returns users ordered by lower case user names.
    assertThat(users).extracting(User::getId).containsExactly(testUser1.getId(), testUser2.getId());
  }

  @Test
  public void testGetByUsernames_CaseInsensitive() {
    User testUser1 = createUser("testUser1");
    User testUser2 = createUser("testUser2");
    List<User> users = userDAO.getByUsernames(Sets.newHashSet("TESTuser1", "testUSER2"));
    // getByUsernames returns users ordered by lower case user names.
    assertThat(users).extracting(User::getId).containsExactly(testUser1.getId(), testUser2.getId());
  }

  @Test
  public void testGetByUsername() {
    User user = userDAO.getByUsername(User.ADMIN_USERNAME);
    assertThat(user).isNotNull();
    assertThat(user.getUsername()).isEqualTo(User.ADMIN_USERNAME);
    assertThat(user.getUsernameLowercase()).isEqualTo(User.ADMIN_USERNAME);
  }

  @Test
  public void testGetByUsername_CaseInsensitive() {
    User user = userDAO.getByUsername("aDMin");
    assertThat(user).isNotNull();
    assertThat(user.getUsername()).isEqualTo(User.ADMIN_USERNAME);
  }

  @Test
  public void testDuplicateUsername_Insert() {
    createUser("testDuplicateUsername");

    assertThatThrownBy(() -> createUser("TESTDuplicateUsername")).isInstanceOf(InvalidNameException.class)
        .hasMessage("TESTDuplicateUsername is already used as a username.");
  }

  @Test
  public void testDuplicateUsername_Update() {
    createUser("testDuplicateUsername");
    User user1 = createUser("testDuplicateUsername1");

    user1.setUsername("TESTDuplicateUsername");
    assertThatThrownBy(() -> userDAO.update(user1)).isInstanceOf(InvalidNameException.class)
        .hasMessage("TESTDuplicateUsername is already used as a username.");
  }

  @Test
  public void testValidateNullUsername_Insert() {
    assertThatThrownBy(() -> createUser(null /* username */)).isInstanceOf(InvalidNameException.class)
        .hasMessage("The username is required.");
  }

  @Test
  public void testValidateNullUsername_Update() {
    User user = createUser("testValidateNullUsername");

    user.setUsername(null);
    assertThat(user.getUsernameLowercase()).isNull();
    assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidNameException.class)
        .hasMessage("The username is required.");
  }

  @Test
  public void testValidateEmptyUsername_Insert() {
    assertThatThrownBy(() -> createUser("")).isInstanceOf(InvalidNameException.class)
        .hasMessage("The username is required.");
  }

  @Test
  public void testValidateEmptyUsername_Update() {
    User user = createUser("testValidateEmptyUsername");
    user.setUsername("");
    assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidNameException.class)
        .hasMessage("The username is required.");
  }

  @Test
  public void testValidateUsernameInvalidChars_Insert() {
    for (String username : NameHelperTest.INVALID_CHARACTERS) {
      assertThatThrownBy(() -> createUser(username)).isInstanceOf(InvalidNameException.class)
          .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "The username", username.charAt(0));
    }
  }

  @Test
  public void testValidateUsernameInvalidChars_Update() {
    User user = createUser("testValidateUsernameInvalidChars");
    for (String username : NameHelperTest.INVALID_CHARACTERS) {
      user.setUsername(username);
      assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidNameException.class)
          .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "The username", username.charAt(0));
    }
  }

  @Test
  public void testInsert_ValidateNameValidChars() {
    for (String name : NameHelperTest.VALID_NAMES) {
      if (name.contains(" ")) {
        continue;
      }
      tempEntity.newUser(name);
    }
  }

  @Test
  public void testUpdate_ValidateNameValidChars() {
    User user = tempEntity.newUser("a");
    for (String name : NameHelperTest.VALID_NAMES) {
      if (name.contains(" ")) {
        continue;
      }
      user.setUsername(name);
      userDAO.update(user);
    }
  }

  @Test
  public void testValidateUsernameSpaces_Insert() {
    String[] invalidSpacingNames = {" leadingSpace", "trailingSpace ", "space in", "double  space"};
    for (String username : invalidSpacingNames) {
      assertThatThrownBy(() -> createUser(username)).isInstanceOf(InvalidNameException.class)
          .hasMessage("The username cannot contain spaces.");
    }
  }

  @Test
  public void testValidateUsernameSpaces_Update() {
    User user = createUser("testValidateUsernameSpaces");

    String[] invalidSpacingNames = {" leadingSpace", "trailingSpace ", "space in", "double  space"};
    for (String username : invalidSpacingNames) {
      user.setUsername(username);
      assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidNameException.class)
          .hasMessage("The username cannot contain spaces.");
    }
  }

  @Test
  public void testValidateUsernameLength_Insert() {
    String username = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    assertThatThrownBy(() -> createUser(username + "a")).isInstanceOf(InvalidNameException.class)
        .hasMessage("The username must be 60 characters or less.");

    createUser(username);
  }

  @Test
  public void testValidateUsernameLength_Update() {
    User user = createUser("testValidateUsernameLength");

    String username = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    user.setUsername(username + "a");
    assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidNameException.class)
        .hasMessage("The username must be 60 characters or less.");

    user.setUsername(username);
    userDAO.update(user);
  }

  @Test
  public void testValidateNullFirstName_Insert() {
    assertThatThrownBy(
        () -> createUser("username", "password", null /* firstName */, "lastName", "email@localhost")).isInstanceOf(
            InvalidNameException.class).hasMessage("The first name is required.");
  }

  @Test
  public void testValidateNullFirstName_Update() {
    User user = createUser("testValidateNullFirstName");

    user.setFirstName(null);
    assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidNameException.class)
        .hasMessage("The first name is required.");
  }

  @Test
  public void testValidateFirstName_Insert() {
    int i = 0;
    for (String name : NameHelperTest.VALID_NAMES) {
      createUser("username" + (i++), "password", name, "lastName", "email@localhost");
    }
  }

  @Test
  public void testValidateFirstNameSpaces_Insert() {
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      assertThatThrownBy(() -> createUser("username", "password", name, "lastName", "email@localhost"))
          .isInstanceOf(InvalidNameException.class)
          .hasMessage("The first name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testValidateFirstName_Update() {
    User user = createUser("username", "password", "firstName", "lastName", "email@localhost");
    for (String name : NameHelperTest.VALID_NAMES) {
      user.setFirstName(name);
      userDAO.update(user);
    }
  }

  @Test
  public void testValidateFirstNameSpaces_Update() {
    User user = createUser("username", "password", "firstName", "lastName", "email@localhost");
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      user.setFirstName(name);
      assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidNameException.class)
          .hasMessage("The first name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testValidateInvalidFirstName_Insert() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      assertThatThrownBy(() -> createUser("username", "password", name, "lastName", "email@localhost"))
          .isInstanceOf(InvalidNameException.class)
          .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "The first name", name.charAt(0));
    }
  }

  @Test
  public void testValidateInvalidFirstName_Update() {
    User user = createUser("testValidateInvalidFirstName");

    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      assertThatThrownBy(() -> {
        user.setFirstName(name);
        userDAO.update(user);
      }).isInstanceOf(InvalidNameException.class)
          .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "The first name",
              name.charAt(0));
    }
  }

  @Test
  public void testValidateEmptyFirstName_Insert() {
    assertThatThrownBy(() -> createUser("username", "password", "", "lastName", "email@localhost")).isInstanceOf(
        InvalidNameException.class).hasMessage("The first name is required.");
  }

  @Test
  public void testValidateEmptyFirstName_Update() {
    User user = createUser("testValidateEmptyFirstName");
    user.setFirstName("");
    assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidNameException.class)
        .hasMessage("The first name is required.");
  }

  @Test
  public void testValidateFirstNameLength_Insert() {
    String firstName = StringUtils.repeat("a", UserDAO.MAX_FIRST_NAME_SIZE);
    assertThatThrownBy(
        () -> createUser("username", "password", firstName + "a", "lastName", "email@localhost"))
            .isInstanceOf(InvalidNameException.class)
            .hasMessage("The first name must be " + UserDAO.MAX_FIRST_NAME_SIZE + " characters or less.");

    createUser("username", "password", firstName, "lastName", "email@localhost");
  }

  @Test
  public void testValidateFirstNameLength_Update() {
    User user = createUser("testValidateFirstNameLength");

    String firstName = StringUtils.repeat("a", UserDAO.MAX_FIRST_NAME_SIZE);
    user.setFirstName(firstName + "a");
    assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidNameException.class)
        .hasMessage("The first name must be " + UserDAO.MAX_FIRST_NAME_SIZE + " characters or less.");

    user.setFirstName(firstName);
    userDAO.update(user);
  }

  @Test
  public void testValidateLastName_Insert() {
    int i = 0;
    for (String name : NameHelperTest.VALID_NAMES) {
      createUser("username" + (i++), "password", "firstName", name, "email@localhost");
    }
  }

  @Test
  public void testValidateLastNameSpaces_Insert() {
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      assertThatThrownBy(() -> createUser("username", "password", "firstName", name, "email@localhost"))
          .isInstanceOf(InvalidNameException.class)
          .hasMessage("The last name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testValidateLastName_Update() {
    User user = createUser("username", "password", "firstName", "lastName", "email@localhost");
    for (String name : NameHelperTest.VALID_NAMES) {
      user.setLastName(name);
      userDAO.update(user);
    }
  }

  @Test
  public void testValidateLastNameSpaces_Update() {
    User user = createUser("username", "password", "firstName", "lastName", "email@localhost");
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      user.setLastName(name);
      assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidNameException.class)
          .hasMessage("The last name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testValidateNullLastName_Insert() {
    assertThatThrownBy(() -> createUser("username", "password", "firstName", null, "email@localhost"))
        .isInstanceOf(InvalidNameException.class)
        .hasMessage("The last name is required.");
  }

  @Test
  public void testValidateNullLastName_Update() {
    User user = createUser("testValidateNullLastName");

    user.setLastName(null);
    assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidNameException.class)
        .hasMessage("The last name is required.");
  }

  @Test
  public void testValidateInvalidLastName_Insert() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      assertThatThrownBy(() -> createUser("username", "password", "firstName", name, "email@localhost"))
          .isInstanceOf(InvalidNameException.class)
          .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "The last name", name.charAt(0));
    }
  }

  @Test
  public void testValidateInvalidLastName_Update() {
    User user = createUser("testValidateInvalidLastName");

    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      user.setLastName(name);
      assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidNameException.class)
          .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "The last name", name.charAt(0));
    }
  }

  @Test
  public void testValidateEmptyLastName_Insert() {
    assertThatThrownBy(() -> createUser("username", "password", "firstName", "", "email@localhost"))
        .isInstanceOf(InvalidNameException.class)
        .hasMessage("The last name is required.");
  }

  @Test
  public void testValidateEmptyLastName_Update() {
    User user = createUser("testValidateEmptyLastName");
    user.setLastName("");
    assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidNameException.class)
        .hasMessage("The last name is required.");
  }

  @Test
  public void testValidateLastNameLength_Insert() {
    String lastName = StringUtils.repeat("a", UserDAO.MAX_LAST_NAME_SIZE);
    assertThatThrownBy(() -> createUser("username", "password", "firstName", lastName + "a", "email@localhost"))
        .isInstanceOf(InvalidNameException.class)
        .hasMessage("The last name must be " + UserDAO.MAX_LAST_NAME_SIZE + " characters or less.");

    createUser("username", "password", "firstName", lastName, "email@localhost");
  }

  @Test
  public void testValidateLastNameLength_Update() {
    User user = createUser("testValidateLastNameLength");

    String lastName = StringUtils.repeat("a", UserDAO.MAX_LAST_NAME_SIZE);
    user.setLastName(lastName + "a");
    assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidNameException.class)
        .hasMessage("The last name must be " + UserDAO.MAX_LAST_NAME_SIZE + " characters or less.");

    user.setLastName(lastName);
    userDAO.update(user);
  }

  @Test
  public void testValidateEmailLength_Insert() {
    String email = StringUtils.repeat("a", UserDAO.MAX_EMAIL_SIZE);
    assertThatThrownBy(() -> createUser("username", "password", "firstName", "lastName", email + "a"))
        .isInstanceOf(InvalidUserException.class)
        .hasMessage("The email must be " + UserDAO.MAX_EMAIL_SIZE + " characters or less.");

    createUser("username", "password", "firstName", "lastName", email);
  }

  @Test
  public void testValidateEmailLength_Update() {
    User user = createUser("testValidateEmailLength");

    String email = StringUtils.repeat("a", UserDAO.MAX_EMAIL_SIZE);
    user.setEmail(email + "a");
    assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidUserException.class)
        .hasMessage("The email must be " + UserDAO.MAX_EMAIL_SIZE + " characters or less.");

    user.setEmail(email);
    userDAO.update(user);
  }

  @Test
  public void testValidateNullEmail_Insert() {
    assertThatThrownBy(
        () -> createUser("username", "password", "firstname", "lastName", null /* email */))
            .isInstanceOf(InvalidUserException.class)
            .hasMessage("The email is required.");
  }

  @Test
  public void testValidateNullEmail_Update() {
    User user = createUser("testValidateNullEmail");

    user.setEmail(null);
    assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidUserException.class)
        .hasMessage("The email is required.");
  }

  @Test
  public void testValidateEmptyEmail_Insert() {
    assertThatThrownBy(() -> createUser("username", "password", "firstname", "lastName", " " /* email */)).isInstanceOf(
        InvalidUserException.class).hasMessage("The email is required.");
  }

  @Test
  public void testValidateEmptyEmail_Update() {
    User user = createUser("testValidateEmptyEmail");

    user.setEmail(" ");
    assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidUserException.class)
        .hasMessage("The email is required.");
  }

  @Test
  public void testValidateNullPassword_Insert() {
    assertThatThrownBy(
        () -> createUser("username", null /* password */, "firstname", "lastName", "username@localhost"))
            .isInstanceOf(InvalidUserException.class)
            .hasMessage("The password is required.");
  }

  @Test
  public void testValidateNullPassword_Update() {
    User user = createUser("testValidateNullPassword");

    user.setPassword(null);
    assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidUserException.class)
        .hasMessage("The password is required.");
  }

  @Test
  public void testValidateEmptyPassword_Insert() {
    assertThatThrownBy(() -> createUser("username", " " /* password */, "firstname", "lastName", "username@localhost"))
        .isInstanceOf(InvalidUserException.class)
        .hasMessage("The password is required.");
  }

  @Test
  public void testValidateEmptyPassword_Update() {
    User user = createUser("testValidateEmptyPassword");

    user.setPassword(" ");
    assertThatThrownBy(() -> userDAO.update(user)).isInstanceOf(InvalidUserException.class)
        .hasMessage("The password is required.");
  }

  @Test
  public void testDeleteCascadesToMembershipMappings() {
    User user = createUser("testDeleteCascadesToMembershipMappings");
    String roleId = roleDAO.getApplicationRoles().get(0).getId();
    membershipMappingDAO.setMembershipMappingsForContextAndRole("app", roleId,
        Collections.singletonList(new MembershipMapping(user.getUsername(), MemberType.USER)));

    userDAO.delete(user);

    assertThat(membershipMappingDAO.getByUser(user.getUsername())).isEmpty();
  }

  @Test
  public void testDeleteCascadesToUserToken_InternalUser() {
    User user = createUser("testDeleteCascadesToUserToken");
    UserToken userToken = tempEntity.newUserToken(user.getUsername(), User.INTERNAL_REALM_ID);

    userDAO.delete(user);

    assertThat(userTokenDAO.getById(userToken.getId())).isNull();
  }

  @Test
  public void testDeleteCascadesToUserToken_ExternalUser() {
    User user = createUser("testDeleteCascadesToUserToken");
    UserToken userToken = tempEntity.newUserToken(user.getUsername(), "External");

    userDAO.delete(user);

    assertThat(userTokenDAO.getById(userToken.getId())).isNotNull();
  }

  @Test
  public void testDeleteCascadesToDashboardFilter() {
    User user = createUser("testDeleteCascadesToDashboardFilter");
    // Add filter
    DashboardFilter dashboardFilter =
        tempEntity.newDashboardFilter(user.getUsername(), User.INTERNAL_REALM_ID, "TestFilter1", "filter1");
    // Add legacy filter
    DashboardFilter dashboardFilterLegacy =
        tempEntity.newDashboardFilterLegacy(user.getUsername(), "TestFilter2", "filter2");

    userDAO.delete(user);
    assertThat(dashboardFilterDAO.getById(dashboardFilter.getId())).isNull();
    assertThat(dashboardFilterDAO.getById(dashboardFilterLegacy.getId())).isNull();
  }

  @Test
  public void testDeleteCascadesToUserFilter() {
    User user = createUser("testDeleteCascadesToUserFilter");
    // Add filter
    UserFilter userFilter = tempEntity.newUserFilter(user.getUsername(), User.INTERNAL_REALM_ID, "TestFilter1",
        UserFilterType.ADVANCED_LEGAL_PACK_DASHBOARD, "filter1");

    userDAO.delete(user);
    assertThat(userFilterDAO.getById(userFilter.getId())).isNull();
  }

  @Test
  public void testDeleteCascadesToUserViewedProductNotification() {
    User user = createUser("testUsername");
    UserViewedProductNotification userViewedProductNotification =
        tempEntity.newUserViewedProductNotification(user.getUsername(), User.INTERNAL_REALM_ID, "testNotificationId");
    UserViewedProductNotification userViewedProductNotificationLegacy =
        tempEntity.newUserViewedProductNotificationLegacy(user.getUsername(), "testNotificationId");

    userDAO.delete(user);

    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotification.getId())).isNull();
    assertThat(userViewedProductNotificationDAO.getById(userViewedProductNotificationLegacy.getId())).isNull();
  }

  @Test
  public void testDelete_cascadeToUserIdePolicyEvaluation() {
    String username = "testUsername";
    User user = createUser(username);
    tempEntity.newUserIdePolicyEvaluation(username);

    assertThat(userIdePolicyEvaluationDAO.getByUsername(username)).isNotNull();

    userDAO.delete(user);

    assertThat(userIdePolicyEvaluationDAO.getByUsername(username)).isNull();
  }

  @Test
  public void testDelete_cascadeToApiAccessAllowList() {
    // set up apiAccessAllowList
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.API_ACCESS_ALLOW_LIST,
        "[\"user1\",\"user2\"]");

    String username = "testUser";
    User user = createUser(username);
    userDAO.delete(user);
    // apiAccessAllowList is not affected
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.API_ACCESS_ALLOW_LIST)
        .getValue()).isEqualTo("[\"user1\",\"user2\"]");

    username = "user1";
    user = createUser(username);
    userDAO.delete(user);
    // user1 is removed from apiAccessAllowList
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.API_ACCESS_ALLOW_LIST)
        .getValue()).isEqualTo("[\"user2\"]");

    username = "user2";
    user = createUser(username);
    userDAO.delete(user);
    // user2 is removed from apiAccessAllowList, which becomes empty
    assertThat(systemConfigurationPropertyDAO.getByName(SystemConfigurationProperty.API_ACCESS_ALLOW_LIST)).isNull();
  }

  @Test
  public void testFindUsersByName_CaseInsensitive() {
    createUser("FOO", "aaa", "xxx", "xxx", "xxx@xxx.xxx");
    User user0 = createUser("xxx0", "aaa", "FOO", "xxx", "xxx@xxx.xxx");
    User user1 = createUser("xxx1", "aaa", "xxx", "FOO", "xxx@xxx.xxx");
    createUser("xxx2", "aaa", "xxx", "xxx", "FOO@xxx.xxx");
    createUser("xxx3", "aaa", "xxx", "xxx", "xxx@xxx.xxx");

    UserDAO dao = userDAO;
    List<User> users = dao.findUsersByName("%fOo%");
    // we only check first name and last name, so 2 results should be found
    assertThat(users).extracting(User::getUsername).containsExactly(user0.getUsername(), user1.getUsername());
  }

  @Test
  public void testFindUsersByName_notByPassword() {
    createUser("xxx", "foo" /* password */, "xxx", "xxx", "xxx@xxx.xxx");

    List<User> users = userDAO.findUsersByName("foo");
    assertThat(users).isEmpty();
  }

  @Test
  public void testFindUsersByName_MatchesAgainstFullName() {
    createUser("user1", "secret", "John", "Doe", "xxx@xxx.xxx");
    User user2 = createUser("user2", "secret", "Jane", "Doe", "xxx@xxx.xxx");

    List<User> users = userDAO.findUsersByName("Jane Doe");
    assertThat(users).extracting(User::getUsername).containsExactly(user2.getUsername());
  }

  @Test
  public void testGetByRealNames_shouldReturnSortedListOfAnyUserMatchingOneOfTheProvidedRealNames() {
    final var user1 = createUser(
        "user1",
        "xxx",
        "Bob",
        "Vance",
        "bvance@example.com");

    createUser(
        "user2",
        "xxx",
        "James",
        "Smith",
        "smith@example.com");

    final var user3 = createUser(
        "user3",
        "xxx",
        "Tim",
        "Master",

        "tmasters@example.com");

    List<User> users = userDAO.getByRealNames(
        Sets.newHashSet("Bob Vance", "Tim Master", "No Body"));
    assertThat(users).extracting(User::getUsername).containsExactly(user3.getUsername(), user1.getUsername());
  }

  @Test
  public void testGetByEmails_shouldReturnSortedListOfAnyUserMatchingOneOfTheProvidedEmails() {
    final var user1 = createUser(
        "user1",
        "xxx",
        "Bob",
        "Vance",
        "q-bvance@example.com");

    createUser(
        "user2",
        "xxx",
        "James",
        "Smith",
        "z-smith@example.com");

    final var user3 = createUser(
        "user3",
        "xxx",
        "Tim",
        "Master",
        "a-tmasters@example.com");

    List<User> users = userDAO.getByEmails(
        Sets.newHashSet("q-bvance@example.com", "a-tmasters@example.com", "a-not-in-db@example.com"));
    assertThat(users).extracting(User::getUsername).containsExactly(user3.getUsername(), user1.getUsername());
  }

  private User createUser(String username) {
    return createUser(username, username + "Password", username + "First", username + "Last", username
        + "Email@localhost");
  }

  private User createUser(String username, String password, String firstName, String lastName, String email) {
    return tempEntity.newUser(username, password, firstName, lastName, email);
  }
}
