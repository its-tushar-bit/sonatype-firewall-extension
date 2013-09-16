/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.security.User;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
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
  public void testGetByUsernameLowercase() throws Exception {
    UserDAO dao = new UserDAO();
    User user = dao.getByUsernameLowercase(User.ADMIN_USERNAME.toLowerCase(Locale.ENGLISH));
    assertThat(user, notNullValue());
    assertThat(user.getUsername(), is(User.ADMIN_USERNAME));
    assertThat(user.getUsernameLowercase(), is(User.ADMIN_USERNAME));
  }

  @Test
  public void testGetByUsernameLowercase_UppercaseNameDoesNotMatch() throws Exception {
    UserDAO dao = new UserDAO();
    User user = dao.getByUsernameLowercase(User.ADMIN_USERNAME.toUpperCase(Locale.ENGLISH));
    assertThat(user, nullValue());
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
      assertEquals("The username cannot be null or empty", expected.getMessage());
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
      assertEquals("The username cannot be null or empty", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyUsername_Insert() {
    try {
      createUser("");
      fail("Expected InvalidNameException");
    }
    catch (InvalidNameException expected) {
      assertEquals("The username cannot be null or empty", expected.getMessage());
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
      assertEquals("The username cannot be null or empty", expected.getMessage());
    }
  }

  @Test
  public void testValidateUsernameInvalidChars_Insert() {
    String[] invalidAlphaNumericNames = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "_", "+" };
    for (String username : invalidAlphaNumericNames) {
      try {
        createUser(username);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must be alpha numeric.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateUsernameInvalidChars_Update() {
    User user = createUser("testValidateUsernameInvalidChars");
    String[] invalidAlphaNumericNames = { "!", "@", "#", "$", "%", "^", "&", "*", "(", "_", "+" };
    for (String username : invalidAlphaNumericNames) {
      user.setUsername(username);
      try {
        new UserDAO().update(user);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must be alpha numeric.", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateUsernameSpaces_Insert() {
    String[] invalidSpacingNames = { " leadingSpace", "trailingSpace ", "space in" };
    for (String username : invalidSpacingNames) {
      try {
        createUser(username);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("The username cannot contain spaces", expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateUsernameSpaces_Update() {
    User user = createUser("testValidateUsernameSpaces");

    String[] invalidSpacingNames = { " leadingSpace", "trailingSpace ", "space in" };
    for (String username : invalidSpacingNames) {
      user.setUsername(username);
      try {
        new UserDAO().update(user);
        fail("Expected InvalidNameException");
      }
      catch (InvalidNameException expected) {
        assertEquals("The username cannot contain spaces",
            expected.getMessage());
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
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
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
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }

    user.setUsername(username);
    new UserDAO().update(user);
  }

  private User createUser(String username) {
    User user = new User(username, username + "First", username + "Last");
    new UserDAO().insert(user);
    usersToDelete.add(user);
    return user;
  }
}
