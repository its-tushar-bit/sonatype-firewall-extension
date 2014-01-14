/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.User;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @since 1.7
 */
public class UserDAOTest
    extends AbstractDbDAOTest
{
  private List<User> usersToDelete = new ArrayList<User>();

  @After
  public void after() {
    UserDAO dao = new UserDAO();
    for (User user : usersToDelete) {
      user = dao.getById(user.getId());
      if (user != null) {
        dao.delete(user);
      }
    }
  }

  @Test
  public void testCRUD() throws Exception {
    UserDAO dao = new UserDAO();

    String username = "testCRUD";
    // Insert
    User user = createUser(username);
    String userId = user.getId();
    assertThat(userId, notNullValue());

    // Get
    user = dao.getByIdNotNull(userId);
    assertThat(user.getUsername(), is(username));

    // Update
    username += "Updated";
    user.setUsername(username);
    dao.update(user);

    // Get
    user = dao.getByIdNotNull(userId);
    assertThat(user.getUsername(), is(username));

    // Delete
    dao.delete(user);

    // Get
    user = dao.getById(userId);
    assertThat(user, nullValue());
  }

  @Test
  public void testGetByUsername() throws Exception {
    UserDAO dao = new UserDAO();
    User user = dao.getByUsername(User.ADMIN_USERNAME);
    assertThat(user, notNullValue());
    assertThat(user.getUsername(), is(User.ADMIN_USERNAME));
    assertThat(user.getUsernameLowercase(), is(User.ADMIN_USERNAME));
  }

  @Test
  public void testGetByUsername_CaseInsensitive() throws Exception {
    UserDAO dao = new UserDAO();
    User user = dao.getByUsername("aDMin");
    assertThat(user, notNullValue());
    assertThat(user.getUsername(), is(User.ADMIN_USERNAME));
  }

  @Test
  public void testDuplicateUsername_Insert() {
    createUser("testDuplicateUsername");

    try {
      createUser("TESTDuplicateUsername");
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("TESTDuplicateUsername is already used as a username.", expected.getMessage());
    }
  }

  @Test
  public void testDuplicateUsername_Update() {
    createUser("testDuplicateUsername");
    User user1 = createUser("testDuplicateUsername1");

    user1.setUsername("TESTDuplicateUsername");
    try {
      new UserDAO().update(user1);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("TESTDuplicateUsername is already used as a username.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNullUsername_Insert() {
    try {
      createUser(null /* username */);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("The username is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNullUsername_Update() {
    User user = createUser("testValidateNullUsername");

    user.setUsername(null);
    assertNull(user.getUsernameLowercase());
    try {
      new UserDAO().update(user);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("The username is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyUsername_Insert() {
    try {
      createUser("");
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("The username is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyUsername_Update() {
    User user = createUser("testValidateEmptyUsername");
    user.setUsername("");
    try {
      new UserDAO().update(user);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("The username is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateUsernameInvalidChars_Insert() {
    for (String username : INVALID_ALPHANUMERIC) {
      try {
        createUser(username);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("The username must be alpha numeric.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateUsernameInvalidChars_Update() {
    User user = createUser("testValidateUsernameInvalidChars");
    for (String username : INVALID_ALPHANUMERIC) {
      user.setUsername(username);
      try {
        new UserDAO().update(user);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("The username must be alpha numeric.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateUsernameSpaces_Insert() {
    String[] invalidSpacingNames = { " leadingSpace", "trailingSpace ", "space in", "double  space" };
    for (String username : invalidSpacingNames) {
      try {
        createUser(username);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("The username cannot contain spaces.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateUsernameSpaces_Update() {
    User user = createUser("testValidateUsernameSpaces");

    String[] invalidSpacingNames = { " leadingSpace", "trailingSpace ", "space in", "double  space" };
    for (String username : invalidSpacingNames) {
      user.setUsername(username);
      try {
        new UserDAO().update(user);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("The username cannot contain spaces.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateUsernameLength_Insert() {
    String username = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    try {
      createUser(username + "a");
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("The username must be 60 characters or less.", expected.getMessage());
    }

    createUser(username);
  }

  @Test
  public void testValidateUsernameLength_Update() {
    User user = createUser("testValidateUsernameLength");

    String username = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    user.setUsername(username + "a");
    try {
      new UserDAO().update(user);
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("The username must be 60 characters or less.", expected.getMessage());
    }

    user.setUsername(username);
    new UserDAO().update(user);
  }

  @Test
  public void testValidateNullFirstName_Insert() {
    try {
      createUser("username", "password", null /* firstName */, "lastName", "email@localhost");
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The first name is required."));
    }
  }

  @Test
  public void testValidateNullFirstName_Update() {
    User user = createUser("testValidateNullFirstName");

    user.setFirstName(null);
    try {
      new UserDAO().update(user);
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The first name is required."));
    }
  }

  @Test
  public void testValidateEmptyFirstName_Insert() {
    try {
      createUser("username", "password", "", "lastName", "email@localhost");
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The first name is required."));
    }
  }

  @Test
  public void testValidateEmptyFirstName_Update() {
    User user = createUser("testValidateEmptyFirstName");
    user.setFirstName("");
    try {
      new UserDAO().update(user);
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The first name is required."));
    }
  }

  @Test
  public void testValidateFirstNameLength_Insert() {
    String firstName = StringUtils.repeat("a", UserDAO.MAX_FIRST_NAME_SIZE);
    try {
      createUser("username", "password", firstName + "a", "lastName", "email@localhost");
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The first name must be " + UserDAO.MAX_FIRST_NAME_SIZE
          + " characters or less."));
    }

    createUser("username", "password", firstName, "lastName", "email@localhost");
  }

  @Test
  public void testValidateFirstNameLength_Update() {
    User user = createUser("testValidateFirstNameLength");

    String firstName = StringUtils.repeat("a", UserDAO.MAX_FIRST_NAME_SIZE);
    user.setFirstName(firstName + "a");
    try {
      new UserDAO().update(user);
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The first name must be " + UserDAO.MAX_FIRST_NAME_SIZE
          + " characters or less."));
    }

    user.setFirstName(firstName);
    new UserDAO().update(user);
  }

  @Test
  public void testValidateNullLastName_Insert() {
    try {
      createUser("username", "password", "firstName", null, "email@localhost");
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The last name is required."));
    }
  }

  @Test
  public void testValidateNullLastName_Update() {
    User user = createUser("testValidateNullLastName");

    user.setLastName(null);
    try {
      new UserDAO().update(user);
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The last name is required."));
    }
  }

  @Test
  public void testValidateEmptyLastName_Insert() {
    try {
      createUser("username", "password", "firstName", "", "email@localhost");
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The last name is required."));
    }
  }

  @Test
  public void testValidateEmptyLastName_Update() {
    User user = createUser("testValidateEmptyLastName");
    user.setLastName("");
    try {
      new UserDAO().update(user);
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The last name is required."));
    }
  }

  @Test
  public void testValidateLastNameLength_Insert() {
    String lastName = StringUtils.repeat("a", UserDAO.MAX_LAST_NAME_SIZE);
    try {
      createUser("username", "password", "firstName", lastName + "a", "email@localhost");
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The last name must be " + UserDAO.MAX_LAST_NAME_SIZE
          + " characters or less."));
    }

    createUser("username", "password", "firstName", lastName, "email@localhost");
  }

  @Test
  public void testValidateLastNameLength_Update() {
    User user = createUser("testValidateLastNameLength");

    String lastName = StringUtils.repeat("a", UserDAO.MAX_LAST_NAME_SIZE);
    user.setLastName(lastName + "a");
    try {
      new UserDAO().update(user);
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The last name must be " + UserDAO.MAX_LAST_NAME_SIZE
          + " characters or less."));
    }

    user.setLastName(lastName);
    new UserDAO().update(user);
  }

  @Test
  public void testValidateEmailLength_Insert() {
    String email = StringUtils.repeat("a", UserDAO.MAX_EMAIL_SIZE);
    try {
      createUser("username", "password", "firstName", "lastName", email + "a");
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The email must be " + UserDAO.MAX_EMAIL_SIZE + " characters or less."));
    }

    createUser("username", "password", "firstName", "lastName", email);
  }

  @Test
  public void testValidateEmailLength_Update() {
    User user = createUser("testValidateEmailLength");

    String email = StringUtils.repeat("a", UserDAO.MAX_EMAIL_SIZE);
    user.setEmail(email + "a");
    try {
      new UserDAO().update(user);
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The email must be " + UserDAO.MAX_EMAIL_SIZE + " characters or less."));
    }

    user.setEmail(email);
    new UserDAO().update(user);
  }

  @Test
  public void testValidateNullEmail_Insert() {
    try {
      createUser("username", "password", "firstname", "lastName", null /* email */);
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The email is required."));
    }
  }

  @Test
  public void testValidateNullEmail_Update() {
    User user = createUser("testValidateNullEmail");

    user.setEmail(null);
    try {
      new UserDAO().update(user);
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The email is required."));
    }
  }

  @Test
  public void testValidateEmptyEmail_Insert() {
    try {
      createUser("username", "password", "firstname", "lastName", " " /* email */);
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The email is required."));
    }
  }

  @Test
  public void testValidateEmptyEmail_Update() {
    User user = createUser("testValidateEmptyEmail");

    user.setEmail(" ");
    try {
      new UserDAO().update(user);
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The email is required."));
    }
  }

  @Test
  public void testValidateNullPassword_Insert() {
    try {
      createUser("username", null /* password */, "firstname", "lastName", "username@localhost");
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The password is required."));
    }
  }

  @Test
  public void testValidateNullPassword_Update() {
    User user = createUser("testValidateNullPassword");

    user.setPassword(null);
    try {
      new UserDAO().update(user);
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The password is required."));
    }
  }

  @Test
  public void testValidateEmptyPassword_Insert() {
    try {
      createUser("username", " " /* password */, "firstname", "lastName", "username@localhost");
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The password is required."));
    }
  }

  @Test
  public void testValidateEmptyPassword_Update() {
    User user = createUser("testValidateEmptyPassword");

    user.setPassword(" ");
    try {
      new UserDAO().update(user);
      fail("Expected InvalidUserException");
    }
    catch (InvalidUserException expected) {
      assertThat(expected.getMessage(), is("The password is required."));
    }
  }

  @Test
  public void testDeleteCascadesToMembershipMappings() {
    User user = createUser("testValidateEmailLength");
    String roleId = new RoleDAO().getApplicationRoles().get(0).getId();
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    membershipMappingDAO.setMembershipMappingsForContextAndRole("app", roleId,
        Arrays.asList(new MembershipMapping(user.getUsername(), MemberType.USER)));

    new UserDAO().delete(user);

    assertThat(membershipMappingDAO.getByUser(user.getUsername()), is(empty()));
  }

  @Test
  public void testFindUser_CaseInsensitive() {
    createUser("FOO", "aaa", "xxx", "xxx", "xxx@xxx.xxx");
    createUser("xxx", "aaa", "FOO", "xxx", "xxx@xxx.xxx");
    createUser("xxx1", "aaa", "xxx", "FOO", "xxx@xxx.xxx");
    createUser("xxx2", "aaa", "xxx", "xxx", "FOO@xxx.xxx");
    createUser("xxx3", "aaa", "xxx", "xxx", "xxx@xxx.xxx");

    UserDAO dao = new UserDAO();
    List<User> users = dao.findUsersByName("fOo");
    //we only check first name and last name, so 2 results should be found
    assertEquals(2, users.size());
  }

  @Test
  public void testFindUser_notByPassword() {
    createUser("xxx", "foo", "xxx", "xxx", "xxx@xxx.xxx");

    UserDAO dao = new UserDAO();
    List<User> users = dao.findUsersByName("foo");
    assertEquals(0, users.size());
  }

  private User createUser(String username) {
    return createUser(username, username + "Password", username + "First", username + "Last", username
        + "Email@localhost");
  }

  private User createUser(String username, String password, String firstName, String lastName, String email) {
    User user = new User(username, password, firstName, lastName, email);
    new UserDAO().insert(user);
    usersToDelete.add(user);
    return user;
  }
}
