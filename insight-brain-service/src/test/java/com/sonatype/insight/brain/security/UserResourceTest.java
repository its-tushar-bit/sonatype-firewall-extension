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
import static org.hamcrest.Matchers.nullValue;
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
    user.setPasswordHash("testCRUDPassword".toCharArray());
    response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    user = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    assertThat(user.getId(), notNullValue());
    assertUser("testCRUD", "testCRUDFirstName", "testCRUDLastName", "testCRUD@sonatype.com", null /* password */, user);

    // Get all
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    users = fromResponse(response);
    assertThat(users, notNullValue());
    assertThat(users, hasSize(2));
    assertThat(User.ADMIN_USERNAME, is(users.get(0).getUsername()));
    assertUser("testCRUD", "testCRUDFirstName", "testCRUDLastName", "testCRUD@sonatype.com", null /* password */,
        users.get(1));
    UserDAO dao = new UserDAO();
    assertUser("testCRUD", "testCRUDFirstName", "testCRUDLastName", "testCRUD@sonatype.com", "testCRUDPassword",
        dao.getByIdNotNull(user.getId()));

    // Update, no password change
    user.setFirstName("testCRUDFirstNameUpdated");
    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    user = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com",
        null /* password */, user);

    // Get all
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    users = fromResponse(response);
    assertThat(users, notNullValue());
    assertThat(users, hasSize(2));
    assertThat(User.ADMIN_USERNAME, is(users.get(0).getUsername()));
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com",
        null /* password */, users.get(1));
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", "testCRUDPassword",
        dao.getByIdNotNull(user.getId()));

    // Update, password change
    user.setPasswordHash("testCRUDPasswordUpdated".toCharArray());
    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    user = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com",
        null /* password */, user);

    // Get all
    response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    users = fromResponse(response);
    assertThat(users, notNullValue());
    assertThat(users, hasSize(2));
    assertThat(User.ADMIN_USERNAME, is(users.get(0).getUsername()));
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com",
        null /* password */, users.get(1));
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com",
        "testCRUDPasswordUpdated", dao.getByIdNotNull(user.getId()));

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
  }

  private void assertUser(String username, String firstName, String lastName, String email, String password, User actual)
  {
    assertThat(actual.getUsername(), is(username));
    assertThat(actual.getFirstName(), is(firstName));
    assertThat(actual.getLastName(), is(lastName));
    assertThat(actual.getEmail(), is(email));
    // The password must be encrypted
    if (password != null) {
      assertThat(actual.getPasswordHash(), notNullValue());
      assertThat(actual.getPasswordHash(), is(not(password.toCharArray())));
    }
    else {
      assertThat(actual.getPasswordHash(), nullValue());
    }
  }

  private String getServiceURL() {
    return getRestBaseUrl() + UserResource.SERVICE_PATH;
  }
}
