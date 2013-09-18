/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class UserResourceTest
    extends AbstractResourceTest
{
  private List<User> fromResponse(Response response) throws IOException {
    User[] users = JsonHelpers.fromJson(response.getResponseBody(), User[].class);
    if (users == null) {
      return null;
    }
    return Arrays.asList(users);
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
