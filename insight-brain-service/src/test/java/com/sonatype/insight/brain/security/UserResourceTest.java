/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserResource.ChangePasswordDTO;
import com.sonatype.insight.brain.security.UserSessionResource.AuthenticationStatus;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;

import org.junit.Assert;

import com.ning.http.client.Cookie;
import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class UserResourceTest
    extends AbstractResourceTest
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

  private List<User> fromResponse(Response response) throws IOException {
    User[] users = JsonHelpers.fromJson(response.getResponseBody(), User[].class);
    if (users == null) {
      return null;
    }
    return Arrays.asList(users);
  }

  @Test
  public void testNullPassword() throws Exception {
    // Add a user with null password
    User user = new User("testNullPassword", "testNullPasswordFirstName", "testNullPasswordLastName");
    user.setEmail("testNullPassword@sonatype.com");
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    user = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    usersToDelete.add(user);
    assertThat(user.getId(), notNullValue());
    assertUser("testNullPassword", "testNullPasswordFirstName", "testNullPasswordLastName",
        "testNullPassword@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), is(UserResource.FAKE_PASSWORD));
    UserDAO dao = new UserDAO();
    user = dao.getByIdNotNull(user.getId());
    assertUser("testNullPassword", "testNullPasswordFirstName", "testNullPasswordLastName",
        "testNullPassword@sonatype.com", user);
    assertThat(user.getPassword(), nullValue());

    // Update to not null password
    user.setPassword("testNullPassword".toCharArray());
    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    user = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    assertThat(user.getId(), notNullValue());
    assertUser("testNullPassword", "testNullPasswordFirstName", "testNullPasswordLastName",
        "testNullPassword@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), is(UserResource.FAKE_PASSWORD));
    user = dao.getByIdNotNull(user.getId());
    assertUser("testNullPassword", "testNullPasswordFirstName", "testNullPasswordLastName",
        "testNullPassword@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), notNullValue());
    assertThat(String.valueOf(user.getPassword()), is(not(UserResource.FAKE_PASSWORD)));
    assertThat(String.valueOf(user.getPassword()), is(not("testNullPassword")));

    // Update to null password
    user.setPassword(null);
    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    user = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    assertThat(user.getId(), notNullValue());
    assertUser("testNullPassword", "testNullPasswordFirstName", "testNullPasswordLastName",
        "testNullPassword@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), is(UserResource.FAKE_PASSWORD));
    user = dao.getByIdNotNull(user.getId());
    assertUser("testNullPassword", "testNullPasswordFirstName", "testNullPasswordLastName",
        "testNullPassword@sonatype.com", user);
    assertThat(user.getPassword(), nullValue());
  }

  @Test
  public void testCRUD() throws Exception {
    // Get all
    Response response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    List<User> users = fromResponse(response);
    assertThat(users, notNullValue());
    assertThat(users, hasSize(1));
    assertThat(User.ADMIN_USERNAME, is(users.get(0).getUsername()));

    // Add
    User user = new User("testCRUD", "testCRUDFirstName", "testCRUDLastName");
    user.setEmail("testCRUD@sonatype.com");
    user.setPassword("testCRUDPassword".toCharArray());
    response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    user = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    usersToDelete.add(user);
    assertThat(user.getId(), notNullValue());
    assertUser("testCRUD", "testCRUDFirstName", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), is(UserResource.FAKE_PASSWORD));
    UserDAO dao = new UserDAO();
    user = dao.getByIdNotNull(user.getId());
    assertUser("testCRUD", "testCRUDFirstName", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), notNullValue());
    assertThat(String.valueOf(user.getPassword()), is(not(UserResource.FAKE_PASSWORD)));
    assertThat(String.valueOf(user.getPassword()), is(not("testCRUDPassword")));

    // Get all
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    users = fromResponse(response);
    assertThat(users, notNullValue());
    assertThat(users, hasSize(2));
    assertThat(User.ADMIN_USERNAME, is(users.get(0).getUsername()));
    assertUser("testCRUD", "testCRUDFirstName", "testCRUDLastName", "testCRUD@sonatype.com", users.get(1));
    assertThat(String.valueOf(users.get(0).getPassword()), is(UserResource.FAKE_PASSWORD));
    assertThat(String.valueOf(users.get(1).getPassword()), is(UserResource.FAKE_PASSWORD));

    // Update, no password change
    user.setFirstName("testCRUDFirstNameUpdated");
    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    user = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), is(UserResource.FAKE_PASSWORD));
    user = dao.getByIdNotNull(user.getId());
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), notNullValue());
    assertThat(String.valueOf(user.getPassword()), is(not(UserResource.FAKE_PASSWORD)));
    assertThat(String.valueOf(user.getPassword()), is(not("testCRUDPassword")));

    // Get all
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    users = fromResponse(response);
    assertThat(users, notNullValue());
    assertThat(users, hasSize(2));
    assertThat(User.ADMIN_USERNAME, is(users.get(0).getUsername()));
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", users.get(1));
    assertThat(String.valueOf(users.get(0).getPassword()), is(UserResource.FAKE_PASSWORD));
    assertThat(String.valueOf(users.get(1).getPassword()), is(UserResource.FAKE_PASSWORD));

    // Update, password change
    user.setPassword("testCRUDPasswordUpdated".toCharArray());
    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    user = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), is(UserResource.FAKE_PASSWORD));
    user = dao.getByIdNotNull(user.getId());
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), notNullValue());
    assertThat(String.valueOf(user.getPassword()), is(not(UserResource.FAKE_PASSWORD)));
    assertThat(String.valueOf(user.getPassword()), is(not("testCRUDPasswordUpdated")));

    // Get all
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    users = fromResponse(response);
    assertThat(users, notNullValue());
    assertThat(users, hasSize(2));
    assertThat(User.ADMIN_USERNAME, is(users.get(0).getUsername()));
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", users.get(1));
    assertThat(String.valueOf(users.get(0).getPassword()), is(UserResource.FAKE_PASSWORD));
    assertThat(String.valueOf(users.get(1).getPassword()), is(UserResource.FAKE_PASSWORD));

    // Delete
    response = AuthedRestAccess.delete(getServiceURL() + "/" + user.getId());
    assertResponseStatus(204, response);

    // Get all
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    users = fromResponse(response);
    assertThat(users, notNullValue());
    assertThat(users, hasSize(1));
    assertThat(User.ADMIN_USERNAME, is(users.get(0).getUsername()));
    assertThat(String.valueOf(users.get(0).getPassword()), is(UserResource.FAKE_PASSWORD));
  }

  @Test
  public void testDelete_ImmediatelyInvalidateSessionsOfDeletedUser() throws Exception {
    // create some user
    User user = new User("test-user", "testFirstName", "testLastName");
    user.setEmail("test@sonatype.com");
    user.setPassword("test-password".toCharArray());
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    user = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    usersToDelete.add(user);
    assertThat(user.getId(), is(notNullValue()));
    Cookie adminCookie = extractSessionCookie(response);

    // log the user in
    response = AuthedRestAccess.post(getRestBaseUrl() + UserSessionResource.SERVICE_PATH, user.getUsername(),
        "test-password");
    assertResponseStatus(204, response);
    Cookie userCookie = extractSessionCookie(response);

    // delete the user
    response = AuthedRestAccess.delete(getServiceURL() + "/" + user.getId());
    assertResponseStatus(204, response);

    // the user's session should be invalid now
    response = RestAccess.get(getRestBaseUrl() + UserSessionResource.SERVICE_PATH, userCookie);
    assertResponseStatus(401, response);

    // the admin's session should not have been invalidated
    response = RestAccess.get(getRestBaseUrl() + UserSessionResource.SERVICE_PATH, adminCookie);
    assertResponseStatus(200, response);
    AuthenticationStatus status = JsonHelpers.fromJson(response.getResponseBody(), AuthenticationStatus.class);
    assertThat(status.isAuthenticated(), is(true));
  }

  @Test
  public void testDelete_Self() throws Exception {
    // create some user
    User user = new User("test-user", "testFirstName", "testLastName");
    user.setEmail("test@sonatype.com");
    user.setPassword("test-password".toCharArray());
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    user = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    usersToDelete.add(user);

    // log the user in
    response = AuthedRestAccess.post(getRestBaseUrl() + UserSessionResource.SERVICE_PATH, user.getUsername(),
        "test-password");
    assertResponseStatus(204, response);
    Cookie userCookie = extractSessionCookie(response);

    // try to delete the user using the same user's session/cookie
    response = AuthedRestAccess.delete(getServiceURL() + "/" + user.getId(), userCookie);
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("Cannot delete the currently logged in user."));
  }

  @Test
  public void testChangePassword() throws Exception {
    // Add user so we can change his password
    User user = new User("testChangePassword", "testChangePasswordFirstName", "testChangePasswordLastName");
    user.setEmail("testChangePassword@sonatype.com");
    user.setPassword("testChangePasswordPassword".toCharArray());
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    user = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    usersToDelete.add(user);

    String changePasswordUrl = getServiceURL() + "/" + user.getId() + "/password";

    // Can't change password when password input doesn't match
    ChangePasswordDTO dto = new ChangePasswordDTO();
    dto.oldPassword = "badPass".toCharArray();
    dto.newPassword = "doesntmatter".toCharArray();

    response = AuthedRestAccess.put(changePasswordUrl, JsonHelpers.asJson(dto));
    assertResponseStatus(400, response);

    // Can change password with correct input
    dto.oldPassword = "testChangePasswordPassword".toCharArray();

    response = AuthedRestAccess.put(changePasswordUrl, JsonHelpers.asJson(dto));
    assertResponseStatus(204, response);
  }

  private void assertUser(String username, String firstName, String lastName, String email, User actual) {
    assertThat(actual.getUsername(), is(username));
    assertThat(actual.getFirstName(), is(firstName));
    assertThat(actual.getLastName(), is(lastName));
    assertThat(actual.getEmail(), is(email));
  }

  private String getServiceURL() {
    return getRestBaseUrl() + UserResource.SERVICE_PATH;
  }
}
