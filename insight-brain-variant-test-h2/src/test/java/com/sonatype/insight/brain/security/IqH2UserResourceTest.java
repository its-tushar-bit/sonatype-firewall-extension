/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserService.ChangePasswordDTO;
import com.sonatype.insight.brain.security.UserService.FindMembersDTO;
import com.sonatype.insight.brain.security.UserSessionResource.AuthenticationStatus;
import com.sonatype.insight.brain.version.VersionResource;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2UserResourceTest
{
  // Injected by IqH2ServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private MembershipMappingDAO membershipMappingDAO;

  private UserDAO dao;

  @BeforeEach
  void setUp() {
    membershipMappingDAO = ctx.lookup(MembershipMappingDAO.class);
    dao = ctx.lookup(UserDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(UserResource.RESOURCE_PATH);
  }

  private HttpRequest sessionRequest() {
    return ctx.restRequest().path(UserSessionResource.RESOURCE_PATH).anon();
  }

  private HttpRequest findRequest(OwnerType ownerType, String ownerId, String query) {
    return restRequest().path("{ownerType}/{ownerId}/query").query("q", query).parameter(ownerType, ownerId);
  }

  private List<User> fromResponse(HttpResponse response) {
    User[] users = response.getBody(User[].class);
    if (users == null) {
      return null;
    }
    return Arrays.asList(users);
  }

  @Test
  void testCRUD() throws Exception {
    // Get all
    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);
    List<User> users = fromResponse(response);
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getUsername()).isEqualTo(User.ADMIN_USERNAME);

    // Add
    User user = new User("testCRUD", "testCRUDPassword", "testCRUDFirstName", "testCRUDLastName",
        "testCRUD@sonatype.com");
    response = restRequest().body(user).post();
    ctx.assertResponseStatus(200, response);
    user = response.getBody(User.class);
    assertThat(user.getId()).isNotNull();
    assertUser("testCRUD", "testCRUDFirstName", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword())).isEqualTo(UserService.FAKE_PASSWORD);
    User expectedUser = dao.getByIdNotNull(user.getId());
    assertUser("testCRUD", "testCRUDFirstName", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(expectedUser.getPassword())).isNotNull();
    assertThat(String.valueOf(expectedUser.getPassword())).isNotEqualTo(UserService.FAKE_PASSWORD);
    assertThat(String.valueOf(expectedUser.getPassword())).isNotEqualTo("testCRUDPassword");

    // Get all
    response = restRequest().get();
    ctx.assertResponseStatus(200, response);
    users = fromResponse(response);
    assertThat(users).hasSize(2);
    assertThat(users.get(0).getUsername()).isEqualTo(User.ADMIN_USERNAME);
    assertUser("testCRUD", "testCRUDFirstName", "testCRUDLastName", "testCRUD@sonatype.com", users.get(1));
    assertThat(String.valueOf(users.get(0).getPassword())).isEqualTo(UserService.FAKE_PASSWORD);
    assertThat(String.valueOf(users.get(1).getPassword())).isEqualTo(UserService.FAKE_PASSWORD);

    // Update, no password change
    user.setFirstName("testCRUDFirstNameUpdated");
    response = restRequest().body(user).put();
    ctx.assertResponseStatus(200, response);
    user = response.getBody(User.class);
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(user.getPassword())).isEqualTo(UserService.FAKE_PASSWORD);
    expectedUser = dao.getByIdNotNull(user.getId());
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", user);
    assertThat(String.valueOf(expectedUser.getPassword())).isNotNull();
    assertThat(String.valueOf(expectedUser.getPassword())).isNotEqualTo(UserService.FAKE_PASSWORD);
    assertThat(String.valueOf(expectedUser.getPassword())).isNotEqualTo("testCRUDPassword");

    // Get all
    response = restRequest().get();
    ctx.assertResponseStatus(200, response);
    users = fromResponse(response);
    assertThat(users).hasSize(2);
    assertThat(users.get(0).getUsername()).isEqualTo(User.ADMIN_USERNAME);
    assertUser("testCRUD", "testCRUDFirstNameUpdated", "testCRUDLastName", "testCRUD@sonatype.com", users.get(1));
    assertThat(String.valueOf(users.get(0).getPassword())).isEqualTo(UserService.FAKE_PASSWORD);
    assertThat(String.valueOf(users.get(1).getPassword())).isEqualTo(UserService.FAKE_PASSWORD);

    // Delete
    response = restRequest().path("{userId}").parameter(user.getId()).delete();
    ctx.assertResponseStatus(204, response);

    // Get all
    response = restRequest().get();
    ctx.assertResponseStatus(200, response);
    users = fromResponse(response);
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getUsername()).isEqualTo(User.ADMIN_USERNAME);
    assertThat(String.valueOf(users.get(0).getPassword())).isEqualTo(UserService.FAKE_PASSWORD);
  }

  @Test
  void testDelete_ImmediatelyInvalidateSessionsOfDeletedUser() throws Exception {
    // create some user
    User user = new User("test-user", "test-password", "testFirstName", "testLastName", "test@sonatype.com");
    HttpResponse response = restRequest().body(user).post();
    ctx.assertResponseStatus(200, response);
    user = response.getBody(User.class);
    assertThat(user.getId()).isNotNull();

    // log the admin in
    response = sessionRequest().auth().post();
    ctx.assertResponseStatus(204, response);
    HttpCookie adminCookie = response.getSessionCookie();
    assertThat(adminCookie).isNotNull();

    // log the user in
    response = sessionRequest().auth(user.getUsername(), "test-password").post();
    ctx.assertResponseStatus(204, response);
    HttpCookie userCookie = response.getSessionCookie();
    assertThat(userCookie).isNotNull();

    // delete the user
    response = restRequest().path("{userId}").parameter(user.getId()).delete();
    ctx.assertResponseStatus(204, response);

    // the user's session should be invalid now
    response = sessionRequest().cookie(userCookie).get();
    ctx.assertResponseStatus(401, response);

    // the admin's session should not have been invalidated
    response = sessionRequest().cookie(adminCookie).get();
    ctx.assertResponseStatus(200, response);
    AuthenticationStatus status = response.getBody(AuthenticationStatus.class);
    assertThat(status.isAuthenticated()).isTrue();
  }

  @Test
  void testDelete_SessionExpired() throws Exception {
    // Shiro validates sessions periodically (see DefaultWebSessionManager.setSessionValidationInterval).
    // This means sessionDAO.getActiveSessions() may return sessions that are already expired, but were not effectively
    // expired by Shiro.
    // If we try to delete a user and logout a subject with an expired session, shiro throws an ExpiredSessionException.
    // This test verifies that we handle that exception.

    // Create some user
    User user = new User("test-user", "test-password", "testFirstName", "testLastName", "test@sonatype.com");
    HttpResponse response = restRequest().body(user).post();
    ctx.assertResponseStatus(200, response);
    user = response.getBody(User.class);
    assertThat(user.getId()).isNotNull();

    DefaultWebSessionManager defaultWebSessionManager = ctx.lookup(DefaultWebSessionManager.class);
    long globalSessionTimeout = defaultWebSessionManager.getGlobalSessionTimeout();
    try {
      // Set the session timeout to 1 second
      defaultWebSessionManager.setGlobalSessionTimeout(1000);

      // Log the user in
      response = sessionRequest().auth(user.getUsername(), "test-password").post();
      ctx.assertResponseStatus(204, response);
      HttpCookie userCookie = response.getSessionCookie();
      assertThat(userCookie).isNotNull();

      // Wait for the session to expire
      Thread.sleep(1001);

      // Delete the user
      response = restRequest().path("{userId}").parameter(user.getId()).delete();
      ctx.assertResponseStatus(204, response);

      // The user's session should be invalid now
      response = sessionRequest().cookie(userCookie).get();
      ctx.assertResponseStatus(401, response);
    }
    finally {
      defaultWebSessionManager.setGlobalSessionTimeout(globalSessionTimeout);
    }
  }

  @Test
  void testDelete_NoNPEWhenUserDeleted() throws Exception {
    // create some user
    User user = new User("test-user", "test-password", "testFirstName", "testLastName", "test@sonatype.com");
    HttpResponse response = restRequest().body(user).post();
    ctx.assertResponseStatus(200, response);
    user = response.getBody(User.class);
    assertThat(user.getId()).isNotNull();

    // create another user
    User user2 = new User("test-user-two", "test-password-two", "testFirstNameTwo", "testLastNameTwo",
        "test2@sonatype.com");
    response = restRequest().body(user2).post();
    ctx.assertResponseStatus(200, response);
    user2 = response.getBody(User.class);
    assertThat(user2.getId()).isNotNull();

    // log the first user in to create a session
    response = sessionRequest().auth(user.getUsername(), "test-password").post();
    ctx.assertResponseStatus(204, response);

    // log the second user in to create another session then log them out
    response = sessionRequest().auth(user2.getUsername(), "test-password-two").post();
    ctx.assertResponseStatus(204, response);
    HttpCookie userCookie = response.getSessionCookie();
    assertThat(userCookie).isNotNull();
    response = sessionRequest().path(UserSessionResource.LOGOUT_PATH).cookie(userCookie).delete();
    ctx.assertResponseStatus(204, response);

    // access an anonymous resource to create a third session
    response = ctx.restRequest().path(VersionResource.RESOURCE_PATH).get();
    ctx.assertResponseStatus(200, response);

    // now delete the first user
    response = restRequest().path("{userId}").parameter(user.getId()).delete();
    ctx.assertResponseStatus(204, response);

    // now delete the second user, if this passes, we are all set, this is where the NPE was occurring prior to fix
    response = restRequest().path("{userId}").parameter(user2.getId()).delete();
    ctx.assertResponseStatus(204, response);
  }

  @Test
  void testDelete_Self() throws Exception {
    // create some user
    User user = new User("test-user", "test-password", "testFirstName", "testLastName", "test@sonatype.com");
    HttpResponse response = restRequest().body(user).post();
    ctx.assertResponseStatus(200, response);
    user = response.getBody(User.class);
    MembershipMapping membershipMapping = new MembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID,
        Role.SYSTEM_ADMIN_ROLE_ID, user.getUsername(), MemberType.USER);
    membershipMappingDAO.insert(membershipMapping);

    // log the user in
    response = sessionRequest().auth(user.getUsername(), "test-password").post();
    ctx.assertResponseStatus(204, response);
    HttpCookie userCookie = response.getSessionCookie();
    assertThat(userCookie).isNotNull();

    // try to delete the user using the same user's session/cookie
    response = restRequest().path("{userId}").parameter(user.getId()).cookie(userCookie).anon().delete();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("A user who is logged in cannot delete themself.");
  }

  @Test
  void testChangeMyPassword() throws Exception {
    // Add user so we can change his password
    User user = new User("testChangePassword", "testChangePasswordPassword", "testChangePasswordFirstName",
        "testChangePasswordLastName", "testChangePassword@sonatype.com");
    HttpResponse response = restRequest().body(user).post();
    ctx.assertResponseStatus(200, response);
    user = response.getBody(User.class);

    HttpRequest request = restRequest().path(UserResource.MY_PASSWORD_PATH)
        .auth(user.getUsername(),
            "testChangePasswordPassword");

    // Can't change password when password input doesn't match
    ChangePasswordDTO dto = new ChangePasswordDTO();
    dto.oldPassword = "badPass";
    dto.newPassword = "doesntmatter";

    response = request.body(dto).put();
    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("Current password is wrong.");

    // Can change password with correct input
    dto.oldPassword = "testChangePasswordPassword";

    response = request.body(dto).put();
    ctx.assertResponseStatus(204, response);
  }

  @Test
  void testResetPassword() throws Exception {
    // Add user so we can change his password
    User user = ctx.tempEntity().newUser("testResetPassword");
    user.setPassword("testResetPasswordPassword");

    HttpResponse response = restRequest().path(UserResource.RESET_PASSWORD_PATH).parameter(user.getId()).put();
    ctx.assertResponseStatus(200, response);

    ChangePasswordDTO dto = response.getBody(ChangePasswordDTO.class);
    assertThat(dto.newPassword).hasSize(12);
    assertThat(StringUtils.isAlphanumeric(dto.newPassword)).isTrue();
  }

  @Test
  void testFindMembersForGlobalRoles() throws Exception {
    HttpResponse response = findRequest(OwnerType.GLOBAL, "global", User.ADMIN_USERNAME + "*").get();
    assertMember(response, null, MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost", "IQ Server");
  }

  @Test
  void testFindMembersForNonGlobalRoles() throws Exception {
    Organization org = ctx.tempEntity().newOrganization();
    HttpResponse response = findRequest(OwnerType.ORGANIZATION, org.getId(), User.ADMIN_USERNAME + "*").get();
    assertMember(response, null, MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost", "IQ Server");
  }

  @Test
  void testShouldDisplayDefaultPasswordWarning() throws Exception {
    HttpRequest request = restRequest();
    HttpResponse response = request.path(UserResource.SHOULD_DISPLAY_DEFAULT_PASSWORD_WARNING).get();
    ctx.assertResponseStatus(HttpStatus.SC_OK, response);
    assertThat(response.getBodyText()).isEqualTo("true");
  }

  private void assertUser(String username, String firstName, String lastName, String email, User actual) {
    assertThat(actual.getUsername()).isEqualTo(username);
    assertThat(actual.getFirstName()).isEqualTo(firstName);
    assertThat(actual.getLastName()).isEqualTo(lastName);
    assertThat(actual.getEmail()).isEqualTo(email);
  }

  private void assertMember(
      HttpResponse response,
      String error,
      MemberType type,
      String name,
      String displayName,
      String email,
      String realm)
  {
    ctx.assertResponseStatus(200, response);

    FindMembersDTO dto = response.getBody(FindMembersDTO.class);

    assertThat(dto.getError()).isEqualTo(error);

    Member[] members = dto.getMembers().toArray(new Member[0]);
    assertThat(members).hasSize(1);
    assertMember(members[0], type, name, displayName, email, realm);
  }

  private void assertMember(
      final Member member,
      final MemberType type,
      final String name,
      final String displayName,
      final String email,
      final String realm)
  {
    assertThat(member.getType()).isEqualTo(type);
    assertThat(member.getInternalName()).isEqualTo(name);
    assertThat(member.getDisplayName()).isEqualTo(displayName);
    assertThat(member.getEmail()).isEqualTo(email);
    assertThat(member.getRealm()).isEqualTo(realm);
  }
}
