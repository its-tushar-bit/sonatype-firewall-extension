/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.NamingException;

import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.TestLdapServer;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.common.collect.Sets;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class UserDirectoryTest
    extends AbstractComponentTest
{

  @Rule
  public TestLdapServer embeddedLdapServer = new TestLdapServer();
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();
  @Inject
  private UserDirectory userDirectory;

  @Test
  public void testGetMembersByNames() throws Exception {
    // Configure LDAP.
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserDirectoryTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    // Add a new CLM user.
    tempEntity.newUser("clmbob");

    // Get users only, no groups.
    Set<String> names = Sets.newHashSet("clmbob", "testuser", "Alpha");
    List<Member> members = userDirectory.getMembersByNames(names, false).get();

    assertThat(members, hasSize(2));
    assertThat(names, hasItems(members.get(0).getInternalName(), members.get(1).getInternalName()));

    // Get both groups and users.
    members = userDirectory.getMembersByNames(names, true).get();

    assertThat(members, hasSize(3));
    assertThat(names,
        containsInAnyOrder(members.get(0).getInternalName(), members.get(1).getInternalName(), members.get(2)
            .getInternalName()));

    // Get users only, case insensitive.
    names = Sets.newHashSet("CLMBOB", "TESTUSER", "ALPHA");
    members = userDirectory.getMembersByNames(names, false).get();

    assertThat(members, hasSize(2));
    assertThat(
        names,
        hasItems(members.get(0).getInternalName().toUpperCase(Locale.ENGLISH), members.get(1).getInternalName()
            .toUpperCase(Locale.ENGLISH)));

    // Get users and groups, case insensitive.
    members = userDirectory.getMembersByNames(names, true).get();

    assertThat(members, hasSize(3));
    assertThat(
        names,
        containsInAnyOrder(members.get(0).getInternalName().toUpperCase(Locale.ENGLISH), members.get(1)
            .getInternalName().toUpperCase(Locale.ENGLISH), members.get(2).getInternalName()
            .toUpperCase(Locale.ENGLISH)));

  }
  
  @Test
  public void testGetMembersByNames_WithUserDirectoryError() throws Exception {
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    NamingException namingException = new NamingException("Naming Exception!");
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenThrow(namingException);

    UserDirectory userDirectory = new UserDirectory(new UserDAO(), mockLdapManager);

    // Add a new CLM user.
    tempEntity.newUser("clmbob");

    Set<String> names = Sets.newHashSet("clmbob", "testuser", "Alpha");
    UserDirectory.QueryResult result = userDirectory.getMembersByNames(names, false);
    List<Member> members = result.get();
    
    // Verify that only the CLM user has been returned.
    assertThat(members, hasSize(1));
    assertThat(names, hasItems(members.get(0).getInternalName()));
    assertEquals(result.getException(), namingException);
  }

  @Test
  public void testGetMembersByNames_WithNullOrEmptyNames() throws Exception {
    // Configure LDAP.
    embeddedLdapServer.start();

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    List<Member> members = userDirectory.getMembersByNames(new HashSet<String>(), false).get();

    assertThat(members, hasSize(0));

    members = userDirectory.getMembersByNames(null, true).get();

    assertThat(members, hasSize(0));

    String nullName = null;
    members = userDirectory.getMembersByNames(Sets.newHashSet(nullName), true).get();

    assertThat(members, hasSize(0));
  }

  @Test
  public void testGetMembersByQuery() throws Exception {
    // Configure LDAP.
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserDirectoryTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    // Add a new CLM user.
    tempEntity.newUser("clmbob", "clm", "bob", "clmbob@bob");

    // Get CLM user.
    List<Member> members = userDirectory.getMembersByQuery("clm ", false).get();

    assertThat(members, hasSize(1));
    assertThat(members.get(0).getInternalName(), is("clmbob"));

    // Get CLM user case insensitive.
    members = userDirectory.getMembersByQuery("CLM ", false).get();

    assertThat(members, hasSize(1));
    assertThat(members.get(0).getInternalName(), is("clmbob"));

    // Get both groups and users.
    members = userDirectory.getMembersByQuery("John Doe", true).get();

    assertThat(members, hasSize(1));
    assertThat(members.get(0).getInternalName(), is("testuser"));

    // Get both groups and users, case insensitive.
    members = userDirectory.getMembersByQuery("JOHN DOE", true).get();

    assertThat(members, hasSize(1));
    assertThat(members.get(0).getInternalName(), is("testuser"));
  }

  @Test
  public void testGetMembersByQuery_WithLdapError() throws Exception {
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    NamingException namingException = new NamingException("Naming Exception!");
    when(mockLdapManager.findUsersByName(any(String.class), anyInt())).thenThrow(namingException);

    UserDirectory userDirectory = new UserDirectory(new UserDAO(), mockLdapManager);

    // Add a new CLM user.
    tempEntity.newUser("testclmuser", "John", "Doe", "testclmuser@testclmuser");

    UserDirectory.QueryResult result = userDirectory.getMembersByQuery("John ", false);
    List<Member> members = result.get();

    // Verify that only the CLM user has been returned.
    assertThat(members, hasSize(1));
    assertThat(members.get(0).getInternalName(), is("testclmuser"));
    assertEquals(result.getException(), namingException);
  }

  @Test
  public void testGetMembersByQuery_WithNullOrEmptyQuery() throws Exception {
    // Configure LDAP.
    embeddedLdapServer.start();

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    List<Member> members = userDirectory.getMembersByQuery(null, false).get();

    assertThat(members, hasSize(0));

    members = userDirectory.getMembersByQuery("", true).get();

    assertThat(members, hasSize(0));
  }

  @Test
  public void testValidateUsers_NullSet() {
    Set<String> invalidUsers = userDirectory.validateUsers(null);
    assertThat(invalidUsers, hasSize(0));
  }

  @Test
  public void testValidateUsers_EmptySet() {
    Set<String> users = Collections.emptySet();
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers, hasSize(0));
  }

  @Test
  public void testValidateUsers_ClmUserFound() {
    User testUser = tempEntity.newUser();

    Set<String> users = Collections.singleton(testUser.getUsername());
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers, hasSize(0));
  }

  @Test
  public void testValidateUsers_Case_Insensitive() {
    User testUser = tempEntity.newUser("TestUser1");

    // Test with a found user.
    Set<String> users = Collections.singleton(testUser.getUsername().toLowerCase(Locale.ENGLISH));
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers, hasSize(0));

    // Test with invalid users.
    users = Sets.newHashSet("Bob", "Sue", "Mary");
    invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers, hasSize(3));

    // Ensure that the names returned match the users input.
    assertEquals(users, invalidUsers);
  }

  @Test
  public void testValidateUsers_LdapUserFound() throws Exception {
    // Configure LDAP.
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserDirectoryTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    Set<String> users = Sets.newHashSet("testuser");
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers, hasSize(0));
  }

  @Test
  public void testValidateUsers_ClmUserNotFound_LdapNotConfigured() {
    Set<String> users = Sets.newHashSet("invaliduser");
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers, hasSize(1));
    assertThat(invalidUsers, contains("invaliduser"));
  }

  @Test
  public void testValidateUsers_ClmUserNotFound_LdapConfigured() throws Exception {
    // Configure LDAP.
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserDirectoryTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    Set<String> users = Sets.newHashSet("invaliduser");
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers, hasSize(1));
    assertThat(invalidUsers, contains("invaliduser"));
  }

  @Test
  public void testValidateUsers_LdapErrorOnGetUsers() throws Exception {
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenThrow(new NamingException("Naming Exception!"));

    UserDirectory userDirectory = new UserDirectory(new UserDAO(), mockLdapManager);

    Set<String> users = Sets.newHashSet("invaliduser");
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers, hasSize(1));
    assertThat(invalidUsers, contains("invaliduser"));
  }

  @Test
  public void testIsLdapUser() throws Exception {
    // Configure LDAP.
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserDirectoryTest/ldap_users.ldif");

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    assertTrue(userDirectory.isLdapUser(new User("testuser", null, null, null, null)));
    assertFalse(userDirectory.isLdapUser(new User("not-a-real-user", null, null, null, null)));
  }
}
