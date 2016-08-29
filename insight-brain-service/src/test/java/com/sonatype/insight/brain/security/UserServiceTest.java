/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;

import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.ldap.TestLdapServer;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserService.ChangePasswordDTO;
import com.sonatype.insight.brain.security.UserService.FindMembersDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.inject.Binder;
import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest
    extends AbstractComponentTest
{
  @Inject
  private UserService userService;

  private SessionDAO sessionDAOMock = mock(SessionDAO.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(SessionDAO.class).toInstance(sessionDAOMock);
    super.configure(binder);
  }

  @Rule
  public TestLdapServer embeddedLdapServer = new TestLdapServer();

  private ApplicationDAO applicationDao = new ApplicationDAO();

  @Test
  public void testDeleteUserNoLdapRemovesContact() throws Exception {
    // Create a user
    String clmUserName = "test-user";
    User user = tempEntity.newUser(clmUserName);

    // Create an application with the user as the contact
    Application application = createApplication(user);
    // Check to see that the contact is the userName
    assertThat(application.getContactInternalName(), is(clmUserName));

    // Delete the user
    userService.deleteUser(user.getId());

    // Check to see if the contact has also been deleted
    application = applicationDao.getById(application.getId());
    assertThat(application, notNullValue());
    assertThat(application.getContactInternalName(), is(nullValue()));
  }

  @Test
  public void testDeleteUserNoLdapMatchRemovesContact() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserServiceTest/ldap_users.ldif");

    final LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    final String clmUserName = "clm-test-user";

    // Make sure the clm user is not in LDAP
    final FindMembersDTO findMembersDTO = userService
        .findMembersForRoles(OwnerType.GLOBAL, null, clmUserName, false /* groupsEnabled */);
    final List<Member> dtoMembers = findMembersDTO.getMembers();
    final Member[] members = dtoMembers.toArray(new Member[dtoMembers.size()]);
    assertThat(members, emptyArray());

    // Add the user to CLM
    final User user = tempEntity.newUser(clmUserName);

    // Create an application with the user as the contact
    Application application = createApplication(user);
    // Check to see that the contact is the userName
    assertThat(application.getContactInternalName(), is(clmUserName));

    // Delete the user
    userService.deleteUser(user.getId());

    // Check to see if the application contact has also been deleted
    application = applicationDao.getById(application.getId());
    assertThat(application, notNullValue());
    assertThat(application.getContactInternalName(), is(nullValue()));
  }

  @Test
  public void testDeleteUserWithLdapMatchDoesNotRemoveContact() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserServiceTest/ldap_users.ldif");

    final LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    final String clmAndLdapUserName = "johndoe";

    // Check LDAP for the user
    FindMembersDTO findMembersDTO = userService
        .findMembersForRoles(OwnerType.GLOBAL, null, "John Doe", false /* groupsEnabled */);
    assertMember(findMembersDTO, null, MemberType.USER, clmAndLdapUserName, "John Doe", "test.user@company.com", "LDAP");

    // Create the same user in CLM
    final User user = tempEntity.newUser(clmAndLdapUserName);

    // Create an application with the user as the contact
    Application application = createApplication(user);
    // Check to see that the contact is the userName
    assertThat(application.getContactInternalName(), is(clmAndLdapUserName));

    // Delete the user
    userService.deleteUser(user.getId());

    // Check to see if the application contact has not been deleted
    application = applicationDao.getById(application.getId());
    assertThat(application, notNullValue());
    assertThat(application.getContactInternalName(), is(clmAndLdapUserName));
  }

  private Application createApplication(User contactUser) {
    // Create an organization and application
    final Organization organization = tempEntity.newOrganization();
    return tempEntity.newApplication("My App", "my-app", organization.getId(), contactUser.getUsername());
  }

  @Test
  public void testResetPassword() throws Exception {
    User user = tempEntity.newUser("testResetPassword");
    user.setPassword("testResetPasswordPassword");

    ChangePasswordDTO dto = userService.resetPassword(user.getId());
    assertThat(dto.newPassword.length(), is(12));
    assertThat(StringUtils.isAlphanumeric(dto.newPassword), is(true));
  }

  @Test
  public void testFindMembersForGlobalRoles_EmptyQuery() {
    try {
      userService.findMembersForRoles(OwnerType.GLOBAL, null, "" /* query */, false /* groupsEnabled */);
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("No search term specified."));
    }
  }

  @Test
  public void testFindMembersForNonGlobalRoles_EmptyQuery() {
    Organization org = tempEntity.newOrganization();
    try {
      userService.findMembersForRoles(OwnerType.ORGANIZATION, org.getId(), "" /* query */, false /* groupsEnabled */);
    }
    catch (BadRequestException expected) {
      assertThat(expected.getMessage(), is("No search term specified."));
    }
  }

  @Test
  public void testFindIqUsers() throws Exception {
    FindMembersDTO findMembersDTO = userService.findMembersForRoles(OwnerType.GLOBAL, null, User.ADMIN_USERNAME + "*",
        false /* groupsEnabled */);
    assertMember(findMembersDTO, null, MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost",
        "IQ Server");

    findMembersDTO = userService.findMembersForRoles(OwnerType.GLOBAL, null,
        User.ADMIN_USERNAME.substring(0, User.ADMIN_USERNAME.length() - 1) + "*", false /* groupsEnabled */);
    assertMember(findMembersDTO, null, MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost",
        "IQ Server");

    findMembersDTO = userService
        .findMembersForRoles(OwnerType.GLOBAL, null, "nobody-has-such-a-name-really*", false /* groupsEnabled */);
    assertThat(findMembersDTO.getError(), nullValue());
    assertThat(findMembersDTO.getMembers(), is(notNullValue()));
    assertThat(findMembersDTO.getMembers(), hasSize(0));
  }

  @Test
  public void testFindLdapUser() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserServiceTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    FindMembersDTO findMembersDTO = userService
        .findMembersForRoles(OwnerType.GLOBAL, null, "John Doe", false /* groupsEnabled */);
    assertMember(findMembersDTO, null, MemberType.USER, "johndoe", "John Doe", "test.user@company.com", "LDAP");

    tempEntity.newUser("johndoe");

    // Test shading. johndoe loaded from "/UserServiceTest/ldap_users.ldif" should not be returned
    findMembersDTO = userService.findMembersForRoles(OwnerType.GLOBAL, null, "John Doe", false /* groupsEnabled */);
    assertMember(findMembersDTO, null, MemberType.USER, "johndoe", "John Doe", "johndoe@void.com", "IQ Server");
  }

  @Test
  public void testFindLdapGroup() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserServiceTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    FindMembersDTO findMembersDTO = userService
        .findMembersForRoles(OwnerType.GLOBAL, null, "Alpha", true /* groupsEnabled */);
    assertMember(findMembersDTO, null, MemberType.GROUP, "Alpha", "Alpha", null, "LDAP");
  }

  @Test
  public void testFindLdapUserAndGroupWithSameName() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserServiceTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    FindMembersDTO findMembersDTO = userService
        .findMembersForRoles(OwnerType.GLOBAL, null, "Beta", true /* groupsEnabled */);
    List<Member> members = findMembersDTO.getMembers();
    assertThat(members, is(notNullValue()));
    assertThat("Found members:" + members, members, hasSize(2));

    assertMember(members.get(0), MemberType.USER, "Beta", "Beta", "beta.user@company.com", "LDAP");
    assertMember(members.get(1), MemberType.GROUP, "Beta", "Beta", null, "LDAP");
  }

  @Test
  public void testNoLdapConnection() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    FindMembersDTO findMembersDTO = userService.findMembersForRoles(OwnerType.GLOBAL, null, User.ADMIN_USERNAME + "*",
        false /* groupsEnabled */);

    // Should not try to use Ldap until server is added and configured
    assertMember(findMembersDTO, null, MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost",
        "IQ Server");

    tempEntity.newLdapConnection(ldapServer.getId());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    findMembersDTO = userService
        .findMembersForRoles(OwnerType.GLOBAL, null, User.ADMIN_USERNAME + "*", false /* groupsEnabled */);
    assertMember(findMembersDTO, "LDAP error, displaying partial results.", MemberType.USER, User.ADMIN_USERNAME,
        "Admin BuiltIn", "admin@localhost", "IQ Server");
  }

  private void assertMember(FindMembersDTO findMembersDTO,
                            String error,
                            MemberType type,
                            String name,
                            String displayName,
                            String email,
                            String realm)
  {
    if (!StringUtils.isBlank(error)) {
      assertThat(findMembersDTO.getError(), is(error));
    }
    else {
      assertThat(findMembersDTO.getError(), nullValue());
    }

    Member[] members = findMembersDTO.getMembers().toArray(new Member[0]);
    assertThat(members, is(notNullValue()));
    assertThat(members.length, is(1));
    assertMember(members[0], type, name, displayName, email, realm);
  }

  private void assertMember(final Member member,
                            final MemberType type,
                            final String name,
                            final String displayName,
                            final String email,
                            final String realm)
  {
    assertThat(member.getType(), is(type));
    assertThat(member.getInternalName(), is(name));
    assertThat(member.getDisplayName(), is(displayName));
    assertThat(member.getEmail(), is(email));
    assertThat(member.getRealm(), is(realm));
  }
}
