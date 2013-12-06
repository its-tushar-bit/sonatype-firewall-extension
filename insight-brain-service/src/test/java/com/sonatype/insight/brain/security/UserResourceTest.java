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
import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.ldap.test.TestLdapServer;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserResource.ChangePasswordDTO;
import com.sonatype.insight.brain.security.UserResource.FindMembersDTO;
import com.sonatype.insight.brain.security.UserSessionResource.AuthenticationStatus;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.version.VersionResource;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Cookie;
import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Rule;
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
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private List<User> usersToDelete = new ArrayList<User>();

  @Rule
  public TestLdapServer embeddedLdapServer = new TestLdapServer();

  @After
  public void after() throws Exception {
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
  public void testNullOrEmptyPassword() throws Exception {
    // Add a user with null password
    User user = new User("testNullPassword", null /* password */, "testNullPasswordFirstName",
        "testNullPasswordLastName", "testNullPassword@sonatype.com");
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("The password is required."));

    // Add a user with empty password
    user = new User("testNullPassword", " " /* password */, "testNullPasswordFirstName",
        "testNullPasswordLastName", "testNullPassword@sonatype.com");
    response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("The password is required."));

    // Create a valid user
    user = new User("testNullPassword", "testNullPassword", "testNullPasswordFirstName", "testNullPasswordLastName",
        "testNullPassword@sonatype.com");
    response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    usersToDelete.add(user);

    // Update to null password
    user.setPassword(null);
    response = AuthedRestAccess.put(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("The password is required."));

    // Update to empty password
    user.setPassword(" ");
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), is("The password is required."));
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
    User user = new User("testCRUD", "testCRUDPassword", "testCRUDFirstName", "testCRUDLastName",
        "testCRUD@sonatype.com");
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
    user.setPassword("testCRUDPasswordUpdated");
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
    User user = new User("test-user", "test-password", "testFirstName", "testLastName", "test@sonatype.com");
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
  public void testDelete_NoNPEWhenUserDeleted() throws Exception {
    // create some user
    User user = new User("test-user", "test-password", "testFirstName", "testLastName", "test@sonatype.com");
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    user = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    usersToDelete.add(user);
    assertThat(user.getId(), is(notNullValue()));
    
    // create another user
    User user2 = new User("test-user-two", "test-password-two", "testFirstNameTwo", "testLastNameTwo", "test2@sonatype.com");
    response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(user2));
    assertResponseStatus(200, response);
    user2 = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    usersToDelete.add(user2);
    assertThat(user2.getId(), is(notNullValue()));
    
    // log the first user in to create a session
    response = AuthedRestAccess.post(getRestBaseUrl() + UserSessionResource.SERVICE_PATH, user.getUsername(),
        "test-password");
    assertResponseStatus(204, response);
    
    // log the second user in to create another session then log them out
    response = AuthedRestAccess.post(getRestBaseUrl() + UserSessionResource.SERVICE_PATH, user2.getUsername(),
        "test-password-two");
    assertResponseStatus(204, response);
    Cookie userCookie = extractSessionCookie(response);
    response = RestAccess.delete(getRestBaseUrl() + UserSessionResource.SERVICE_PATH, null, null, null, userCookie);
    assertResponseStatus(204, response);
    
    // access an anonymous resource to create a third session
    response = RestAccess.get(getRestBaseUrl() + VersionResource.SERVICE_PATH);
    assertResponseStatus(200, response);
    
    // now delete the first user
    response = AuthedRestAccess.delete(getServiceURL() + "/" + user.getId());
    assertResponseStatus(204, response);
    
    // now delete the second user, if this passes, we are all set, this is where the NPE was occurring prior to fix
    response = AuthedRestAccess.delete(getServiceURL() + "/" + user2.getId());
    assertResponseStatus(204, response);
  }

  @Test
  public void testDelete_Self() throws Exception {
    // create some user
    User user = new User("test-user", "test-password", "testFirstName", "testLastName", "test@sonatype.com");
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    user = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    usersToDelete.add(user);
    MembershipMapping membershipMapping = new MembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID,
        Role.ADMIN_ROLE_ID, user.getUsername(), MemberType.USER);
    new MembershipMappingDAO().insert(membershipMapping);

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
  public void testChangeMyPassword() throws Exception {
    // Add user so we can change his password
    User user = new User("testChangePassword", "testChangePasswordPassword", "testChangePasswordFirstName",
        "testChangePasswordLastName", "testChangePassword@sonatype.com");
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(user));
    assertResponseStatus(200, response);
    user = JsonHelpers.fromJson(response.getResponseBody(), User.class);
    usersToDelete.add(user);

    String changePasswordUrl = getServiceURL() + "/password";

    // Can't change password when password input doesn't match
    ChangePasswordDTO dto = new ChangePasswordDTO();
    dto.oldPassword = "badPass";
    dto.newPassword = "doesntmatter";

    response = AuthedRestAccess.put(changePasswordUrl, JsonHelpers.asJson(dto), user.getUsername(),
        "testChangePasswordPassword");
    assertResponseStatus(400, response);
    assertEquals("Current password is wrong.", response.getResponseBody());

    // Can change password with correct input
    dto.oldPassword = "testChangePasswordPassword";

    response = AuthedRestAccess.put(changePasswordUrl, JsonHelpers.asJson(dto), user.getUsername(),
        "testChangePasswordPassword");
    assertResponseStatus(204, response);
  }
  
  @Test
  public void testResetPassword() throws Exception {
    // Add user so we can change his password
    User user = tempEntity.newUser("testResetPassword");
    user.setPassword("testResetPasswordPassword");
    
    String url = getServiceURL() + "/" + user.getId() + "/reset";

    Response response = AuthedRestAccess.put(url, null);
    assertResponseStatus(200, response);
    
    ChangePasswordDTO dto = JsonHelpers.fromJson(response.getResponseBody(), ChangePasswordDTO.class);
    assertThat(dto.newPassword.length(), is(12));
    assertThat(StringUtils.isAlphanumeric(dto.newPassword), is(true));
  }

  @Test
  public void testFindCLMUsers() throws Exception {
    Response response = AuthedRestAccess.get(getSearchUrl(""));
    assertResponseStatus(400, response);

    response = AuthedRestAccess.get(getSearchUrl(User.ADMIN_USERNAME));
    assertMember(response, null, MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost", "CLM");

    response = AuthedRestAccess.get(getSearchUrl(User.ADMIN_USERNAME.substring(0, User.ADMIN_USERNAME.length() - 1)));
    assertMember(response, null, MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost", "CLM");

    response = AuthedRestAccess.get(getSearchUrl("nobody-has-such-a-name-really"));
    assertResponseStatus(200, response);

    FindMembersDTO dto = fromJson(response, FindMembersDTO.class);
    assertThat(dto.getError(), nullValue());

    Member[] users = dto.getMembers().toArray(new Member[0]);
    assertThat(users, is(notNullValue()));
    assertThat(users.length, is(0));
  }

  @Test
  public void testFindLdapUser() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserResourceTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    Response response = AuthedRestAccess.get(getSearchUrl("John"));
    assertMember(response, null, MemberType.USER, "testuser", "John Doe", "test.user@company.com", "LDAP");

    tempEntity.newUser("testuser");

    // Test shading. testuser loaded from "/UserResourceTest/ldap_users.ldif" should not be returned
    response = AuthedRestAccess.get(getSearchUrl("John"));
    assertMember(response, null, MemberType.USER, "testuser", "John Doe", "testuser@void.com", "CLM");
  }

  @Test
  public void testFindLdapGroup() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserResourceTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    Response response = AuthedRestAccess.get(getSearchUrl("Alpha"));
    assertMember(response, null, MemberType.GROUP, "Alpha", "Alpha", null, "LDAP");
  }

  @Test
  public void testFindLdapUserGroupSameName() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserResourceTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    Response response = AuthedRestAccess.get(getSearchUrl("Beta"));
    assertResponseStatus(200, response);
    FindMembersDTO dto = fromJson(response, FindMembersDTO.class);
    Member[] members = dto.getMembers().toArray(new Member[0]);
    assertThat(members, is(notNullValue()));
    assertThat(members.length, is(2));

    assertMember(members[0], MemberType.USER, "Beta", "Beta User", "beta.user@company.com", "LDAP");
    assertMember(members[1], MemberType.GROUP, "Beta", "Beta", null, "LDAP");
  }

  @Test
  public void testNoLdapConnection() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    Response response = AuthedRestAccess.get(getSearchUrl(User.ADMIN_USERNAME));

    // Should not try to use Ldap until server is added and configured
    assertMember(response, null, MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost", "CLM");

    tempEntity.newLdapConnection(ldapServer.getId());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    response = AuthedRestAccess.get(getSearchUrl(User.ADMIN_USERNAME));
    assertMember(response, "LDAP connection unavailable. Displaying local users only.", MemberType.USER,
        User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost", "CLM");
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

  private String getSearchUrl(String query) {
    return getRestBaseUrl() + UserResource.SERVICE_PATH + "/global/global/query?q=" + query;
  }

  private void assertMember(Response response, String error, MemberType type, String name, String displayName,
                            String email, String realm) throws IOException
  {
    assertResponseStatus(200, response);

    FindMembersDTO dto = fromJson(response, FindMembersDTO.class);

    if (!StringUtils.isBlank(error)) {
      assertThat(dto.getError(), is(error));
    } else {
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
