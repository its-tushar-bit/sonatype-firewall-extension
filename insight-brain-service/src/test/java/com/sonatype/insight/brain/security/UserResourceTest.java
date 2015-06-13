/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserService.ChangePasswordDTO;
import com.sonatype.insight.brain.security.UserService.FindMembersDTO;
import com.sonatype.insight.brain.security.UserSessionResource.AuthenticationStatus;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.brain.version.VersionResource;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class UserResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(UserResource.SERVICE_PATH);
  }

  private HttpRequest sessionRequest() {
    return super.restRequest().path(UserSessionResource.SERVICE_PATH).anon();
  }

  private HttpRequest findRequest(String ownerType, String ownerId, String query) {
    return restRequest().path("{ownerType}/{ownerId}/query").query("q", "{pattern}")
        .parameter(ownerType, ownerId, query);
  }

  private List<User> fromResponse(HttpResponse response) {
    User[] users = fromJson(response, User[].class);
    if (users == null) {
      return null;
    }
    return Arrays.asList(users);
  }

  @Test
  public void testCRUD() throws Exception {
    // Get all
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    List<User> users = fromResponse(response);
    assertThat(users, notNullValue());
    assertThat(users, hasSize(1));
    assertThat(User.ADMIN_USERNAME, is(users.get(0).getUsername()));

    // Add
    User user = new User("testCRUD", "testCRUDPassword", "testCRUDFirstName", "testCRUDLastName",
        "testCRUD@sonatype.com");
    response = restRequest().body(user).post();
    assertResponseStatus(200, response);
    user = fromJson(response, User.class);
    tempEntity.register(user);
    assertThat(user.getId(), notNullValue());
    assertUser("testCRUD", "testCRUDFirstName", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), is(UserService.FAKE_PASSWORD));
    UserDAO dao = new UserDAO();
    user = dao.getByIdNotNull(user.getId());
    assertUser("testCRUD", "testCRUDFirstName", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), notNullValue());
    assertThat(String.valueOf(user.getPassword()), is(not(UserService.FAKE_PASSWORD)));
    assertThat(String.valueOf(user.getPassword()), is(not("testCRUDPassword")));

    // Get all
    response = restRequest().get();
    assertResponseStatus(200, response);
    users = fromResponse(response);
    assertThat(users, notNullValue());
    assertThat(users, hasSize(2));
    assertThat(User.ADMIN_USERNAME, is(users.get(0).getUsername()));
    assertUser("testCRUD", "testCRUDFirstName", "testCRUDLastName", "testCRUD@sonatype.com", users.get(1));
    assertThat(String.valueOf(users.get(0).getPassword()), is(UserService.FAKE_PASSWORD));
    assertThat(String.valueOf(users.get(1).getPassword()), is(UserService.FAKE_PASSWORD));

    // Update, no password change
    user.setFirstName("testCRUDFirstNameUpdated");
    response = restRequest().body(user).put();
    assertResponseStatus(200, response);
    user = fromJson(response, User.class);
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), is(UserService.FAKE_PASSWORD));
    user = dao.getByIdNotNull(user.getId());
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), notNullValue());
    assertThat(String.valueOf(user.getPassword()), is(not(UserService.FAKE_PASSWORD)));
    assertThat(String.valueOf(user.getPassword()), is(not("testCRUDPassword")));

    // Get all
    response = restRequest().get();
    assertResponseStatus(200, response);
    users = fromResponse(response);
    assertThat(users, notNullValue());
    assertThat(users, hasSize(2));
    assertThat(User.ADMIN_USERNAME, is(users.get(0).getUsername()));
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", users.get(1));
    assertThat(String.valueOf(users.get(0).getPassword()), is(UserService.FAKE_PASSWORD));
    assertThat(String.valueOf(users.get(1).getPassword()), is(UserService.FAKE_PASSWORD));

    // Update, password change
    user.setPassword("testCRUDPasswordUpdated");
    response = restRequest().body(user).put();
    assertResponseStatus(200, response);
    user = fromJson(response, User.class);
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), is(UserService.FAKE_PASSWORD));
    user = dao.getByIdNotNull(user.getId());
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword()), notNullValue());
    assertThat(String.valueOf(user.getPassword()), is(not(UserService.FAKE_PASSWORD)));
    assertThat(String.valueOf(user.getPassword()), is(not("testCRUDPasswordUpdated")));

    // Get all
    response = restRequest().get();
    assertResponseStatus(200, response);
    users = fromResponse(response);
    assertThat(users, notNullValue());
    assertThat(users, hasSize(2));
    assertThat(User.ADMIN_USERNAME, is(users.get(0).getUsername()));
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", users.get(1));
    assertThat(String.valueOf(users.get(0).getPassword()), is(UserService.FAKE_PASSWORD));
    assertThat(String.valueOf(users.get(1).getPassword()), is(UserService.FAKE_PASSWORD));

    // Delete
    response = restRequest().path("{userId}").parameter(user.getId()).delete();
    assertResponseStatus(204, response);

    // Get all
    response = restRequest().get();
    assertResponseStatus(200, response);
    users = fromResponse(response);
    assertThat(users, notNullValue());
    assertThat(users, hasSize(1));
    assertThat(User.ADMIN_USERNAME, is(users.get(0).getUsername()));
    assertThat(String.valueOf(users.get(0).getPassword()), is(UserService.FAKE_PASSWORD));
  }

  @Test
  public void testDelete_ImmediatelyInvalidateSessionsOfDeletedUser() throws Exception {
    // create some user
    User user = new User("test-user", "test-password", "testFirstName", "testLastName", "test@sonatype.com");
    HttpResponse response = restRequest().body(user).post();
    assertResponseStatus(200, response);
    user = fromJson(response, User.class);
    tempEntity.register(user);
    assertThat(user.getId(), is(notNullValue()));
    HttpCookie adminCookie = extractSessionCookie(response);

    // log the user in
    response = sessionRequest().auth(user.getUsername(), "test-password").post();
    assertResponseStatus(204, response);
    HttpCookie userCookie = extractSessionCookie(response);

    // delete the user
    response = restRequest().path("{userId}").parameter(user.getId()).delete();
    assertResponseStatus(204, response);

    // the user's session should be invalid now
    response = sessionRequest().cookie(userCookie).get();
    assertResponseStatus(401, response);

    // the admin's session should not have been invalidated
    response = sessionRequest().cookie(adminCookie).get();
    assertResponseStatus(200, response);
    AuthenticationStatus status = fromJson(response, AuthenticationStatus.class);
    assertThat(status.isAuthenticated(), is(true));
  }

  @Test
  public void testDelete_NoNPEWhenUserDeleted() throws Exception {
    // create some user
    User user = new User("test-user", "test-password", "testFirstName", "testLastName", "test@sonatype.com");
    HttpResponse response = restRequest().body(user).post();
    assertResponseStatus(200, response);
    user = fromJson(response, User.class);
    tempEntity.register(user);
    assertThat(user.getId(), is(notNullValue()));

    // create another user
    User user2 = new User("test-user-two", "test-password-two", "testFirstNameTwo", "testLastNameTwo",
        "test2@sonatype.com");
    response = restRequest().body(user2).post();
    assertResponseStatus(200, response);
    user2 = fromJson(response, User.class);
    tempEntity.register(user2);
    assertThat(user2.getId(), is(notNullValue()));

    // log the first user in to create a session
    response = sessionRequest().auth(user.getUsername(), "test-password").post();
    assertResponseStatus(204, response);

    // log the second user in to create another session then log them out
    response = sessionRequest().auth(user2.getUsername(), "test-password-two").post();
    assertResponseStatus(204, response);
    HttpCookie userCookie = extractSessionCookie(response);
    response = sessionRequest().path(UserSessionResource.LOGOUT_PATH).cookie(userCookie).delete();
    assertResponseStatus(204, response);

    // access an anonymous resource to create a third session
    response = super.restRequest().path(VersionResource.SERVICE_PATH).get();
    assertResponseStatus(200, response);

    // now delete the first user
    response = restRequest().path("{userId}").parameter(user.getId()).delete();
    assertResponseStatus(204, response);

    // now delete the second user, if this passes, we are all set, this is where the NPE was occurring prior to fix
    response = restRequest().path("{userId}").parameter(user2.getId()).delete();
    assertResponseStatus(204, response);
  }

  @Test
  public void testDelete_Self() throws Exception {
    // create some user
    User user = new User("test-user", "test-password", "testFirstName", "testLastName", "test@sonatype.com");
    HttpResponse response = restRequest().body(user).post();
    assertResponseStatus(200, response);
    user = fromJson(response, User.class);
    tempEntity.register(user);
    MembershipMapping membershipMapping = new MembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID,
        Role.SYSTEM_ADMIN_ROLE_ID, user.getUsername(), MemberType.USER);
    new MembershipMappingDAO().insert(membershipMapping);

    // log the user in
    response = sessionRequest().auth(user.getUsername(), "test-password").post();
    assertResponseStatus(204, response);
    HttpCookie userCookie = extractSessionCookie(response);

    // try to delete the user using the same user's session/cookie
    response = restRequest().path("{userId}").parameter(user.getId()).cookie(userCookie).anon().delete();
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("Cannot delete the currently logged in user."));
  }

  @Test
  public void testChangeMyPassword() throws Exception {
    // Add user so we can change his password
    User user = new User("testChangePassword", "testChangePasswordPassword", "testChangePasswordFirstName",
        "testChangePasswordLastName", "testChangePassword@sonatype.com");
    HttpResponse response = restRequest().body(user).post();
    assertResponseStatus(200, response);
    user = fromJson(response, User.class);
    tempEntity.register(user);

    HttpRequest request = restRequest().path(UserResource.MY_PASSWORD_PATH).auth(user.getUsername(),
        "testChangePasswordPassword");

    // Can't change password when password input doesn't match
    ChangePasswordDTO dto = new ChangePasswordDTO();
    dto.oldPassword = "badPass";
    dto.newPassword = "doesntmatter";

    response = request.body(dto).put();
    assertResponseStatus(400, response);
    assertEquals("Current password is wrong.", response.getResponseBody());

    // Can change password with correct input
    dto.oldPassword = "testChangePasswordPassword";

    response = request.body(dto).put();
    assertResponseStatus(204, response);
  }

  @Test
  public void testResetPassword() throws Exception {
    // Add user so we can change his password
    User user = tempEntity.newUser("testResetPassword");
    user.setPassword("testResetPasswordPassword");

    HttpResponse response = restRequest().path(UserResource.RESET_PASSWORD_PATH).parameter(user.getId()).put();
    assertResponseStatus(200, response);

    ChangePasswordDTO dto = fromJson(response, ChangePasswordDTO.class);
    assertThat(dto.newPassword.length(), is(12));
    assertThat(StringUtils.isAlphanumeric(dto.newPassword), is(true));
  }

  @Test
  public void testFindMembersForGlobalRoles() throws Exception {
    HttpResponse response = findRequest("global", "global", User.ADMIN_USERNAME + "*").get();
    assertMember(response, null, MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost", "CLM");
  }

  @Test
  public void testFindMembersForNonGlobalRoles() throws Exception {
    Organization org = tempEntity.newOrganization();
    HttpResponse response = findRequest(IdUtils.TYPE_ORGANIZATION, org.getId(), User.ADMIN_USERNAME + "*").get();
    assertMember(response, null, MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost", "CLM");
  }

  private void assertUser(String username, String firstName, String lastName, String email, User actual) {
    assertThat(actual.getUsername(), is(username));
    assertThat(actual.getFirstName(), is(firstName));
    assertThat(actual.getLastName(), is(lastName));
    assertThat(actual.getEmail(), is(email));
  }

  private void assertMember(HttpResponse response, String error, MemberType type, String name, String displayName,
      String email, String realm) throws IOException
  {
    assertResponseStatus(200, response);

    FindMembersDTO dto = fromJson(response, FindMembersDTO.class);

    if (!StringUtils.isBlank(error)) {
      assertThat(dto.getError(), is(error));
    }
    else {
      assertThat(dto.getError(), nullValue());
    }

    Member[] members = dto.getMembers().toArray(new Member[0]);
    assertThat(members, is(notNullValue()));
    assertThat(members.length, is(1));
    assertMember(members[0], type, name, displayName, email, realm);
  }

  private void assertMember(final Member member, final MemberType type, final String name, final String displayName,
      final String email, final String realm)
  {
    assertThat(member.getType(), is(type));
    assertThat(member.getInternalName(), is(name));
    assertThat(member.getDisplayName(), is(displayName));
    assertThat(member.getEmail(), is(email));
    assertThat(member.getRealm(), is(realm));
  }
}
