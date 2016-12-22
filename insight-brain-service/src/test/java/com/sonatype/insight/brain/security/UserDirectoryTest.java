/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.NamingException;

import com.sonatype.insight.brain.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.ldap.LdapGroup;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.ldap.TestLdapServer;
import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserDirectory.QueryResult;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserDirectoryTest
    extends AbstractComponentTest
{
  @Rule
  public TestLdapServer testLdapServer1;

  @Rule
  public TestLdapServer testLdapServer2;

  @Inject
  private LdapManager manager;

  @Inject
  private UserDirectory userDirectory;

  @Before
  public void before() {
    testLdapServer1 = new TestLdapServer(new File(tempDir.getRoot(), "server1"), "/UserDirectoryTest/ldap_users1.ldif");
    testLdapServer2 = new TestLdapServer(new File(tempDir.getRoot(), "server2"), "/UserDirectoryTest/ldap_users2.ldif");
  }

  private void configureAndStartNewLdapServer(TestLdapServer testLdapServer, String ldapServerName)
      throws Exception
  {
    testLdapServer.start();

    LdapServer ldapServer = tempEntity.newLdapServer(ldapServerName);
    tempEntity.newLdapConnection(ldapServer.getId(), testLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());
  }

  @Test
  public void testGetMembersByName() throws Exception {
    // Configure LDAP.
    configureAndStartNewLdapServer(testLdapServer1, "LDAP");
    configureAndStartNewLdapServer(testLdapServer2, "LDAP2");

    // Add a new internal user.
    tempEntity.newUser("clmbob");

    // Get users only, no groups.
    Set<String> names = Sets.newHashSet("clmbob", "testuser1", "Alpha1", "Alpha2");
    List<Member> members = userDirectory.getUsersByName(names).get();

    assertThat(members, hasSize(2));
    assertThat(names, hasItems(members.get(0).getInternalName(), members.get(1).getInternalName()));

    // Get both groups and users.
    members = userDirectory
        .getMembersByName(Sets.newHashSet(createUser("clmbob"), createUser("testuser1"), createGroup("Alpha1"), 
            createGroup("Alpha2"))).get();

    assertThat(members, hasSize(4));
    assertThat(
        names,
        containsInAnyOrder(members.get(0).getInternalName(), members.get(1).getInternalName(), members.get(2)
            .getInternalName(), members.get(3).getInternalName()));

    // Get users only, case insensitive.
    names = Sets.newHashSet("CLMBOB", "TESTUSER1", "ALPHA1", "ALPHA2");
    members = userDirectory.getUsersByName(names).get();

    assertThat(members, hasSize(2));
    assertThat(
        names,
        hasItems(members.get(0).getInternalName().toUpperCase(Locale.ENGLISH), members.get(1).getInternalName()
            .toUpperCase(Locale.ENGLISH)));

    // Get users and groups, case insensitive.
    members = userDirectory
        .getMembersByName(Sets.newHashSet(createUser("CLMBOB"), createUser("TESTUSER1"), createGroup("ALPHA1"), 
            createGroup("ALPHA2"))).get();

    assertThat(members, hasSize(4));
    assertThat(
        names,
        containsInAnyOrder(members.get(0).getInternalName().toUpperCase(Locale.ENGLISH), members.get(1)
            .getInternalName().toUpperCase(Locale.ENGLISH), members.get(2).getInternalName()
            .toUpperCase(Locale.ENGLISH), members.get(3).getInternalName().toUpperCase(Locale.ENGLISH)));
  }

  @Test
  public void testGetDynamicGroupsDisabled() throws Exception {
    testLdapServer1.start();

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    LdapConnection conn = tempEntity.newLdapConnection(ldapServer.getId(), testLdapServer1.getPort());
    LdapUserMapping umap = tempEntity.newLdapUserMapping(ldapServer.getId());

    conn.setSearchBase("dc=company,dc=com");
    manager.saveConnection(conn);

    LdapUserMappingDAO userMappingDAO = new LdapUserMappingDAO();
    umap.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    umap.setDynamicGroupSearchEnabled(false);
    umap.setUserMemberOfGroupAttribute("departmentNumber");
    userMappingDAO.update(umap);
    
    assertThat(manager.isGroupSearchEnabled(ldapServer), is(false));

    QueryResult result = userDirectory.getMembersByQuery("testUsers", true);
    assertThat(result.get(), hasSize(0));

    result = userDirectory.getMembersByName(Collections.singleton(createGroup("testUsers")));
    Assert.assertEquals(1, result.get().size());
    Member member = result.get().get(0);
    assertEquals(MemberType.GROUP, member.getType());
    assertEquals("testUsers", member.getInternalName());
    assertEquals(null, member.getRealm());
  }

  @Test
  public void testGetMembersByName_WithUserDirectory_GetUsersNamingError() throws Exception {
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    tempEntity.newLdapServer("Test Server");
    when(mockLdapManager.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapManager.isGroupSearchEnabled(any(LdapServer.class))).thenReturn(true);
    Throwable namingException = new NamingException("Naming Exception!");
    when(mockLdapManager.getUsers(any(LdapServer.class), any(String[].class), anyInt())).thenThrow(namingException);

    UserDirectory userDirectory = new UserDirectory(new UserDAO(), mockLdapManager);

    // Add a new internal user.
    tempEntity.newUser("clmbob");

    Set<String> names = Sets.newHashSet("clmbob", "testuser1", "Alpha1");
    UserDirectory.QueryResult result = userDirectory.getMembersByName(Sets.newHashSet(createUser("clmbob"), 
        createUser("testuser1"), createGroup("Alpha1")));
    List<Member> members = result.get();

    // Verify that the internal user has been returned.
    assertThat(members, hasSize(1));
    assertThat(names, hasItems(members.get(0).getInternalName()));
    assertThat(result.getException(), instanceOf(NamingException.class));
    assertThat(result.getException().getSuppressed().length, is(1));
    assertThat(result.getException().getSuppressed()[0], instanceOf(NamingException.class));
  }

  @Test
  public void testGetMembersByName_WithUserDirectory_GetUsersGenericError() throws Exception {
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    tempEntity.newLdapServer("Test Server");
    when(mockLdapManager.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapManager.isGroupSearchEnabled(any(LdapServer.class))).thenReturn(true);
    Throwable exception = new RuntimeException("Exception!");
    when(mockLdapManager.getUsers(any(LdapServer.class), any(String[].class), anyInt())).thenThrow(exception);

    UserDirectory userDirectory = new UserDirectory(new UserDAO(), mockLdapManager);

    // Add a new internal user.
    tempEntity.newUser("clmbob");

    Set<String> names = Sets.newHashSet("clmbob", "testuser1", "Alpha1");
    UserDirectory.QueryResult result = userDirectory.getMembersByName(Sets.newHashSet(createUser("clmbob"),
        createUser("testuser1"), createGroup("Alpha1")));
    List<Member> members = result.get();

    // Verify that the internal user has been returned.
    assertThat(members, hasSize(1));
    assertThat(names, hasItems(members.get(0).getInternalName()));
    assertThat(result.getException(), instanceOf(Exception.class));
    assertThat(result.getException().getSuppressed().length, is(1));
    assertThat(result.getException().getSuppressed()[0], instanceOf(Exception.class));
  }

  @Test
  public void testGetMembersByName_WithUserDirectory_GetGroupsNamingError() throws Exception {
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    tempEntity.newLdapServer("Test Server");
    when(mockLdapManager.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapManager.isGroupSearchEnabled(any(LdapServer.class))).thenReturn(true);
    Throwable namingException = new NamingException("Naming Exception!");
    when(mockLdapManager.getGroups(any(LdapServer.class), any(String[].class), anyInt())).thenThrow(namingException);

    UserDirectory userDirectory = new UserDirectory(new UserDAO(), mockLdapManager);

    // Add a new internal user.
    tempEntity.newUser("clmbob");

    Set<String> names = Sets.newHashSet("clmbob", "testuser1", "Alpha1");
    UserDirectory.QueryResult result = userDirectory.getMembersByName(Sets.newHashSet(createUser("clmbob"), 
        createUser("testuser1"), createGroup("Alpha1")));
    List<Member> members = result.get();

    // Verify that the internal user has been returned.
    assertThat(members, hasSize(1));
    assertThat(names, hasItems(members.get(0).getInternalName()));
    assertThat(result.getException(), instanceOf(NamingException.class));
    assertThat(result.getException().getSuppressed().length, is(1));
    assertThat(result.getException().getSuppressed()[0], is(namingException));
  }

  @Test
  public void testGetMembersByName_WithUserDirectory_GetGroupsGenericError() throws Exception {
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    tempEntity.newLdapServer("Test Server");
    when(mockLdapManager.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapManager.isGroupSearchEnabled(any(LdapServer.class))).thenReturn(true);
    Throwable exception = new RuntimeException("Exception!");
    when(mockLdapManager.getGroups(any(LdapServer.class), any(String[].class), anyInt())).thenThrow(exception);

    UserDirectory userDirectory = new UserDirectory(new UserDAO(), mockLdapManager);

    // Add a new internal user.
    tempEntity.newUser("clmbob");

    Set<String> names = Sets.newHashSet("clmbob", "testuser1", "Alpha1");
    UserDirectory.QueryResult result = userDirectory.getMembersByName(Sets.newHashSet(createUser("clmbob"),
        createUser("testuser1"), createGroup("Alpha1")));
    List<Member> members = result.get();

    // Verify that the internal user and testuser1 have been returned.
    assertThat(members, hasSize(1));
    assertThat(names, hasItems(members.get(0).getInternalName()));
    assertThat(result.getException(), instanceOf(Exception.class));
    assertThat(result.getException().getSuppressed().length, is(1));
    assertThat(result.getException().getSuppressed()[0], is(exception));
  }

  @Test
  public void testGetMembersByName_noUnnecessaryQueries() throws Exception {
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    when(mockLdapManager.isLdapEnabled(any(LdapServer.class))).thenReturn(true);

    UserDirectory userDirectory = new UserDirectory(new UserDAO(), mockLdapManager);

    UserDirectory.QueryResult result = userDirectory.getMembersByName(new LinkedList<Member>());

    assertThat(result.get(), hasSize(0));
    verify(mockLdapManager, never()).getUsers(any(LdapServer.class), any(String[].class), any(Long.class));
    verify(mockLdapManager, never()).getGroups(any(LdapServer.class), any(String[].class), any(Long.class));
  }

  @Test
  public void testGetMembersByName_WithNullOrEmptyNames() throws Exception {
    configureAndStartNewLdapServer(testLdapServer1, "LDAP");

    List<Member> members = userDirectory.getMembersByName(new HashSet<Member>()).get();

    assertThat(members, hasSize(0));

    members = userDirectory.getMembersByName(null).get();

    assertThat(members, hasSize(0));

    Member nullUser = new Member(MemberType.USER, null, null);
    Member nullGroup = new Member(MemberType.GROUP, null, null);
    members = userDirectory.getMembersByName(Sets.newHashSet(nullUser, nullGroup)).get();

    assertThat(members, hasSize(0));
  }

  @Test
  public void testGetMembersByQuery() throws Exception {
    // Configure LDAP.
    configureAndStartNewLdapServer(testLdapServer1, "LDAP1");
    configureAndStartNewLdapServer(testLdapServer2, "LDAP2");

    // Add a new internal user.
    tempEntity.newUser("clmbob", "clm", "bob", "clmbob@bob");

    // Get internal user.
    List<Member> members = userDirectory.getMembersByQuery("clm bob", false).get();

    assertThat(members, hasSize(1));
    assertThat(members.get(0).getInternalName(), is("clmbob"));

    // Get internal user case insensitive.
    members = userDirectory.getMembersByQuery("CLM BOB", false).get();

    assertThat(members, hasSize(1));
    assertThat(members.get(0).getInternalName(), is("clmbob"));

    // Get users.
    members = userDirectory.getMembersByQuery("John Doe", true).get();

    assertThat(members, hasSize(2));
    assertThat(members.get(0).getInternalName(), is("testuser1"));
    assertThat(members.get(1).getInternalName(), is("testuser2"));

    // Get users, case insensitive.
    members = userDirectory.getMembersByQuery("JOHN DOE", true).get();

    assertThat(members, hasSize(2));
    assertThat(members.get(0).getInternalName(), is("testuser1"));
    assertThat(members.get(1).getInternalName(), is("testuser2"));

    // Add a new internal user.
    tempEntity.newUser("alphabob", "alphaclm", "bob", "alphaclmbob@bob");
    // Get both groups and users, case insensitive.
    members = userDirectory.getMembersByQuery("ALPHA*", true).get();

    assertThat(members, hasSize(4));
    assertTrue(containsInternalName(members, "alphabob"));
    assertTrue(containsInternalName(members, "Alpha"));
    assertTrue(containsInternalName(members, "Alpha1"));
    assertTrue(containsInternalName(members, "Alpha2"));
  }

  private boolean containsInternalName(Collection<Member> members, String internalName) {
    for (Member member : members) {
      if (member != null && member.getInternalName().equals(internalName)) {
        return true;
      }
    }
    return false;
  }
  
  @Test
  public void testGetMembersByQuery_WithWildcards() throws Exception {
    // Configure LDAP.
    configureAndStartNewLdapServer(testLdapServer1, "LDAP");

    // Add a new internal user.
    tempEntity.newUser("clmbob", "clm", "bob", "clmbob@bob");

    // Prefix wildcard
    List<Member> members = userDirectory.getMembersByQuery("*bob", false).get();
    assertThat(members, hasSize(1));
    assertThat(members.get(0).getInternalName(), is("clmbob"));
    members = userDirectory.getMembersByQuery("*bo", false).get();
    assertThat(members, hasSize(0));

    // Suffix wildcard
    members = userDirectory.getMembersByQuery("clm*", false).get();
    assertThat(members, hasSize(1));
    assertThat(members.get(0).getInternalName(), is("clmbob"));
    members = userDirectory.getMembersByQuery("lm*", false).get();
    assertThat(members, hasSize(0));

    // Prefix and suffix wildcards
    members = userDirectory.getMembersByQuery("*lm bo*", false).get();
    assertThat(members, hasSize(1));
    assertThat(members.get(0).getInternalName(), is("clmbob"));
    members = userDirectory.getMembersByQuery("*lmbo*", false).get();
    assertThat(members, hasSize(0));
  }

  @Test
  public void testGetMembersByQuery_WithLdapError() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    when(mockLdapManager.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    Throwable namingException = new NamingException("Naming Exception!");
    when(mockLdapManager.findUsersByName(argThat(new SameId(ldapServer)), any(String.class), anyInt()))
        .thenThrow(namingException);

    UserDirectory userDirectory = new UserDirectory(new UserDAO(), mockLdapManager);

    // Add a new internal user.
    tempEntity.newUser("testclmuser", "John", "Doe", "testclmuser@testclmuser");

    UserDirectory.QueryResult result = userDirectory.getMembersByQuery("John *", false);
    List<Member> members = result.get();

    // Verify that the internal user has been returned.
    assertThat(members, hasSize(1));
    assertThat(members.get(0).getInternalName(), is("testclmuser"));
    assertThat(result.getException(), instanceOf(NamingException.class));
    assertThat(result.getException().getSuppressed().length, is(1));
    assertThat(result.getException().getSuppressed()[0], is(namingException));
  }

  @Test
  public void testGetMembersByQuery_GroupsWithLdapError() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    when(mockLdapManager.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapManager.isGroupSearchEnabled(argThat(new SameId(ldapServer)))).thenReturn(true);
    Throwable namingException = new NamingException("Naming Exception!");
    when(mockLdapManager.findGroupsByName(argThat(new SameId(ldapServer)), any(String.class), anyInt()))
        .thenThrow(namingException);

    UserDirectory underTest = new UserDirectory(new UserDAO(), mockLdapManager);
    
    UserDirectory.QueryResult result = underTest.getMembersByQuery("Alpha", true);
    List<Member> members = result.get();

    assertThat(members, hasSize(0));
    assertThat(result.getException(), instanceOf(NamingException.class));
    assertThat(result.getException().getSuppressed().length, is(1));
    assertThat(result.getException().getSuppressed()[0], is(namingException));
  }

  @Test
  public void testGetMembersByQuery_WithGenericError() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    when(mockLdapManager.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    Throwable exception = new RuntimeException("Exception!");
    when(mockLdapManager.findUsersByName(argThat(new SameId(ldapServer)), any(String.class), anyInt()))
        .thenThrow(exception);

    UserDirectory userDirectory = new UserDirectory(new UserDAO(), mockLdapManager);

    // Add a new internal user.
    tempEntity.newUser("testclmuser", "John", "Doe", "testclmuser@testclmuser");

    UserDirectory.QueryResult result = userDirectory.getMembersByQuery("John *", false);
    List<Member> members = result.get();

    // Verify that the internal user has been returned.
    assertThat(members, hasSize(1));
    assertThat(members.get(0).getInternalName(), is("testclmuser"));
    assertThat(result.getException(), instanceOf(Exception.class));
    assertThat(result.getException().getSuppressed().length, is(1));
    assertThat(result.getException().getSuppressed()[0], is(exception));
  }

  @Test
  public void testGetMembersByQuery_GroupsWithGenericError() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    when(mockLdapManager.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapManager.isGroupSearchEnabled(argThat(new SameId(ldapServer)))).thenReturn(true);
    Throwable exception = new RuntimeException("Exception!");
    when(mockLdapManager.findGroupsByName(argThat(new SameId(ldapServer)), any(String.class), anyInt()))
        .thenThrow(exception);

    UserDirectory underTest = new UserDirectory(new UserDAO(), mockLdapManager);

    UserDirectory.QueryResult result = underTest.getMembersByQuery("Alpha", true);
    List<Member> members = result.get();

    assertThat(members, hasSize(0));
    assertThat(result.getException(), instanceOf(Exception.class));
    assertThat(result.getException().getSuppressed().length, is(1));
    assertThat(result.getException().getSuppressed()[0], is(exception));
  }

  @Test
  public void testGetMembersByQuery_WithMultipleErrors() throws Exception {
    LdapServer ldapServer1 = tempEntity.newLdapServer("Test Server1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("Test Server2");
    LdapServer ldapServer3 = tempEntity.newLdapServer("Test Server3");
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    when(mockLdapManager.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    // First LDAP server throws a generic exception
    Throwable exception = new RuntimeException("Exception!");
    when(mockLdapManager.findUsersByName(argThat(new SameId(ldapServer1)), any(String.class), anyInt()))
        .thenThrow(exception);
    // Second LDAP server throws a NamingException
    Throwable namingException = new NamingException("NamingException!");
    when(mockLdapManager.findUsersByName(argThat(new SameId(ldapServer2)), any(String.class), anyInt()))
        .thenThrow(namingException);
    // Third LDAP server returns a user
    LdapUser ldapUser = new LdapUser();
    ldapUser.setUsername("testldapuser");
    when(mockLdapManager.findUsersByName(argThat(new SameId(ldapServer3)), any(String.class), anyInt()))
        .thenReturn(Collections.singletonList(ldapUser));

    UserDirectory userDirectory = new UserDirectory(new UserDAO(), mockLdapManager);

    // Add a new internal user.
    tempEntity.newUser("testclmuser", "John", "Doe", "testclmuser@testclmuser");

    UserDirectory.QueryResult result = userDirectory.getMembersByQuery("John *", false);
    List<Member> members = result.get();

    // Verify that the internal user and the LDAP user have been returned.
    assertThat(members, hasSize(2));
    assertThat(members.get(0).getInternalName(), is("testclmuser"));
    assertThat(members.get(1).getInternalName(), is("testldapuser"));
    assertThat(result.getException(), instanceOf(Exception.class));
    assertThat(result.getException().getSuppressed().length, is(2));
    assertThat(result.getException().getSuppressed()[0], is(namingException));
    assertThat(result.getException().getSuppressed()[1], is(exception));
  }

  @Test
  public void testGetMembersByQuery_GroupsWithMultipleErrors() throws Exception {
    LdapServer ldapServer1 = tempEntity.newLdapServer("Test Server1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("Test Server2");
    LdapServer ldapServer3 = tempEntity.newLdapServer("Test Server3");
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    when(mockLdapManager.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapManager.isGroupSearchEnabled(argThat(new SameId(ldapServer1)))).thenReturn(true);
    // First LDAP server throws a generic exception
    Throwable exception = new RuntimeException("Exception!");
    when(mockLdapManager.findGroupsByName(argThat(new SameId(ldapServer1)), any(String.class), anyInt()))
        .thenThrow(exception);
    // Second LDAP server throws a NamingException
    when(mockLdapManager.isGroupSearchEnabled(argThat(new SameId(ldapServer2)))).thenReturn(true);
    Throwable namingException = new NamingException("NamingException!");
    when(mockLdapManager.findGroupsByName(argThat(new SameId(ldapServer2)), any(String.class), anyInt()))
        .thenThrow(namingException);
    // Third LDAP server returns a group
    LdapGroup ldapGroup = new LdapGroup();
    ldapGroup.setGroupname("testldapgroup");
    when(mockLdapManager.isGroupSearchEnabled(argThat(new SameId(ldapServer3)))).thenReturn(true);
    when(mockLdapManager.findGroupsByName(argThat(new SameId(ldapServer3)), any(String.class), anyInt()))
        .thenReturn(Collections.singletonList(ldapGroup));

    UserDirectory underTest = new UserDirectory(new UserDAO(), mockLdapManager);
    
    UserDirectory.QueryResult result = underTest.getMembersByQuery("any", true);
    List<Member> members = result.get();

    // Verify that the LDAP group have been returned.
    assertThat(members, hasSize(1));
    //assertThat(members.get(0).getInternalName(), is("testclmuser"));
    assertThat(members.get(0).getDisplayName(), is("testldapgroup"));
    assertThat(result.getException(), instanceOf(Exception.class));
    assertThat(result.getException().getSuppressed().length, is(2));
    assertThat(result.getException().getSuppressed()[0], is(namingException));
    assertThat(result.getException().getSuppressed()[1], is(exception));
  }

  @Test
  public void testGetUsersByName_LdapOnlyCalledWithNamesNotFoundInInternalRealm() throws Exception {
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    tempEntity.newLdapServer("Test Server");

    when(mockLdapManager.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    List<LdapUser> emptyLdapUsers = new ArrayList<>();
    String[] expectedArgument = new String[] { "Alpha", "CLMBOB" };
    when(mockLdapManager.getUsers(any(LdapServer.class), argThat(is(equalTo(expectedArgument))), eq(2L))).thenReturn(emptyLdapUsers);

    UserDirectory userDirectory = new UserDirectory(new UserDAO(), mockLdapManager);

    // Add a new internal user.
    tempEntity.newUser("testclmuser", "John", "Doe", "testclmuser@testclmuser");

    UserDirectory.QueryResult result = userDirectory.getUsersByName(Sets.newHashSet("tesTcLmUsEr", expectedArgument[0],
        expectedArgument[1]));
    List<Member> members = result.get();

    // Verify that only the internal user has been returned.
    assertThat(members, hasSize(1));
    assertThat(members.get(0).getInternalName(), is("testclmuser"));
    // That 'John' was removed from the user names to search.
    verify(mockLdapManager).getUsers(any(LdapServer.class), argThat(is(equalTo(expectedArgument))), eq(2L));

    // Test that the get users method isn't called when only internal users are provided.
    userDirectory.getUsersByName(Sets.newHashSet("tesTcLmUsEr"));
    // Count of the number of calls is still one, as expected.
    verify(mockLdapManager, times(1)).getUsers(any(LdapServer.class), any(String[].class), anyInt());
  }
  
  @Test
  public void testGetUsersByName_MultipleLdapServers() throws Exception {
    // Configure LDAP.
    configureAndStartNewLdapServer(testLdapServer1, "LDAP1");
    configureAndStartNewLdapServer(testLdapServer2, "LDAP2");

    // Get one user
    List<Member> members = userDirectory.getUsersByName(Sets.newHashSet("testuser1")).get();
    assertThat(members, hasSize(1));
    assertThat(members.get(0).getInternalName(), is("testuser1"));
    assertThat(members.get(0).getRealm(), is("LDAP1"));

    // Get users from both server 1 and server 2
    members = userDirectory.getUsersByName(Sets.newHashSet("testuser1", "testuser2")).get();
    assertThat(members, hasSize(2));
    assertThat(members.get(0).getInternalName(), is("testuser1"));
    assertThat(members.get(0).getRealm(), is("LDAP1"));
    assertThat(members.get(1).getInternalName(), is("testuser2"));
    assertThat(members.get(1).getRealm(), is("LDAP2"));
  }

  @Test
  public void testGetMembersByQuery_WithNullOrEmptyQuery() throws Exception {
    configureAndStartNewLdapServer(testLdapServer1, "LDAP");

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
  public void testValidateUsers_InternalUserFound() {
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
    configureAndStartNewLdapServer(testLdapServer1, "LDAP");

    Set<String> users = Sets.newHashSet("testuser1");
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers, hasSize(0));
  }

  @Test
  public void testValidateUsers_InternalUserNotFound_LdapNotConfigured() {
    Set<String> users = Sets.newHashSet("invaliduser");
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers, hasSize(1));
    assertThat(invalidUsers, contains("invaliduser"));
  }

  @Test
  public void testValidateUsers_InternalUserNotFound_LdapConfigured() throws Exception {
    configureAndStartNewLdapServer(testLdapServer1, "LDAP");

    Set<String> users = Sets.newHashSet("invaliduser");
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers, hasSize(1));
    assertThat(invalidUsers, contains("invaliduser"));
  }

  @Test
  public void testValidateUsers_LdapErrorOnGetUsers() throws Exception {
    LdapManager mockLdapManager = Mockito.mock(LdapManager.class);
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    when(mockLdapManager.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapManager.getUsers(argThat(new SameId(ldapServer)), any(String[].class), anyInt())).thenThrow(new NamingException("Naming Exception!"));

    UserDirectory userDirectory = new UserDirectory(new UserDAO(), mockLdapManager);

    Set<String> users = Sets.newHashSet("invaliduser");
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers, hasSize(1));
    assertThat(invalidUsers, contains("invaliduser"));
  }

  @Test
  public void testIsLdapUser() throws Exception {
    configureAndStartNewLdapServer(testLdapServer1, "LDAP");
    configureAndStartNewLdapServer(testLdapServer2, "LDAP2");

    assertTrue(userDirectory.isLdapUser(new User("testuser1", null, null, null, null)));
    assertTrue(userDirectory.isLdapUser(new User("testuser2", null, null, null, null)));
    assertFalse(userDirectory.isLdapUser(new User("not-a-real-user", null, null, null, null)));
  }

  private static Member createUser(String name) {
    return new Member(MemberType.USER, name, null);
  }

  private static Member createGroup(String name) {
    return new Member(MemberType.GROUP, name, null);
  }

  @Test
  public void testGetMembersByQuery_AuthenticatedUsersGroup_GroupsDisabled() throws Exception {
    List<Member> members = userDirectory.getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME, false).get();
    assertThat(members, hasSize(0));

    members = userDirectory.getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME + "*", false).get();
    assertThat(members, hasSize(0));
  }

  @Test
  public void testGetMembersByQuery_AuthenticatedUsersGroup_GroupsEnabled() throws Exception {
    // Exact name
    List<Member> members = userDirectory.getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME, true).get();
    assertThat(members, hasSize(1));
    assertIsAuthenticatedUsersGroup(members.get(0));

    // With wild card
    members = userDirectory.getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME + "*", true).get();
    assertThat(members, hasSize(1));
    assertIsAuthenticatedUsersGroup(members.get(0));
    members = userDirectory.getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME.substring(0, 5) + "*", true)
        .get();
    assertThat(members, hasSize(1));
    assertIsAuthenticatedUsersGroup(members.get(0));

    // With wild card and special regex chars - should not throw an exception because the regex pattern is incorrect.
    members = userDirectory
        .getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME.substring(0, 5) + "(*", true).get();
    assertThat(members, hasSize(0));

    // Case insensitive
    members = userDirectory.getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME.toLowerCase(Locale.ENGLISH),
        true).get();
    assertThat(members, hasSize(1));
    assertIsAuthenticatedUsersGroup(members.get(0));
    members = userDirectory.getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME.toUpperCase(Locale.ENGLISH),
        true).get();
    assertThat(members, hasSize(1));
    assertIsAuthenticatedUsersGroup(members.get(0));
  }

  private void assertIsAuthenticatedUsersGroup(Member member) {
    assertThat(member.getType(), is(MemberType.GROUP));
    assertThat(member.getDisplayName(), is(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME));
    assertThat(member.getInternalName(), is(Group.AUTHENTICATED_USERS_GROUP_ID));
    assertThat(member.getInternalNameLowerCase(), is(Group.AUTHENTICATED_USERS_GROUP_ID.toLowerCase(Locale.ENGLISH)));
    assertThat(member.getEmail(), is(nullValue()));
    assertThat(member.getRealm(), is(InternalRealm.DISPLAY_NAME));
  }

  @Test
  public void testGetMembersByName_AuthenticatedUsersGroup() throws Exception {
    // Exact name
    List<Member> members = userDirectory.getMembersByName(
        Collections.singleton(createGroup(Group.AUTHENTICATED_USERS_GROUP_ID))).get();
    assertThat(members, hasSize(1));
    assertIsAuthenticatedUsersGroup(members.get(0));

    // Case insensitive
    members = userDirectory.getMembersByName(
        Collections.singleton(createGroup(Group.AUTHENTICATED_USERS_GROUP_ID.toLowerCase(Locale.ENGLISH)))).get();
    assertThat(members, hasSize(1));
    assertIsAuthenticatedUsersGroup(members.get(0));
    members = userDirectory.getMembersByName(
        Collections.singleton(createGroup(Group.AUTHENTICATED_USERS_GROUP_ID.toUpperCase(Locale.ENGLISH)))).get();
    assertThat(members, hasSize(1));
    assertIsAuthenticatedUsersGroup(members.get(0));
  }

  @Test
  public void testGetMembersByQuery_SameUserInMultipleRealms() throws Exception {
    configureAndStartNewLdapServer(testLdapServer1, "LDAP1");

    // Should get back only the user from testLdapServer1.
    List<Member> members = userDirectory.getMembersByQuery("Beta User", false).get();
    assertThat(members, hasSize(1));
    assertThat(members.get(0).getDisplayName(), is("Beta User"));
    assertThat(members.get(0).getRealm(), is("LDAP1"));

    // Start testLdapServer2. Should still get back only the user from testLdapServer1 since it is higher priority.
    configureAndStartNewLdapServer(testLdapServer2, "LDAP2");
    members = userDirectory.getMembersByQuery("Beta User", false).get();
    assertThat(members, hasSize(1));
    assertThat(members.get(0).getDisplayName(), is("Beta User"));
    assertThat(members.get(0).getRealm(), is("LDAP1"));
  
    // Add a new IQ user. Should get back only the IQ user.
    tempEntity.newUser("beta", "Beta", "User", "betauser@example.com");
    members = userDirectory.getMembersByQuery("Beta User", false).get();
    assertThat(members, hasSize(1));
    assertThat(members.get(0).getDisplayName(), is("Beta User"));
    assertThat(members.get(0).getRealm(), is("IQ Server"));
  }

  @Test
  public void testGetMembersByQuery_SameGroupInMultipleRealms() throws Exception {
    configureAndStartNewLdapServer(testLdapServer1, "LDAP1");
    configureAndStartNewLdapServer(testLdapServer2, "LDAP2");

    // Should return all groups from all realms. When same group occurs in both realms a single occurence is retrieved.
    List<Member> members = userDirectory.getMembersByQuery("Alpha*", true).get();
    assertThat(members, hasSize(3));
    assertTrue(containsDisplayName(members, "Alpha"));
    assertTrue(containsDisplayName(members, "Alpha1"));
    assertTrue(containsDisplayName(members, "Alpha2"));
  }

  private boolean containsDisplayName(Collection<Member> members, String displayName) {
    for (Member member : members) {
      if (member != null && member.getDisplayName().equals(displayName)) {
        return true;
      }
    }
    return false;
  }
  
  private static class SameId
      extends ArgumentMatcher<LdapServer>
  {
    private final String ldapServerId;

    SameId(LdapServer ldapServer) {
      ldapServerId = ldapServer.getId();
    }

    @Override
    public boolean matches(Object obj) {
      if (obj == null) {
        return false;
      }
      LdapServer other = (LdapServer) obj;
      return ldapServerId.equals(other.getId());
    }
  }
}
