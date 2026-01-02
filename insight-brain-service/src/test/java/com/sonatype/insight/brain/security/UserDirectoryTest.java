/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.NamingException;

import com.sonatype.insight.brain.configuration.ldap.LdapGroup;
import com.sonatype.insight.brain.configuration.ldap.LdapService;
import com.sonatype.insight.brain.configuration.ldap.LdapUser;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.security.AbstractSsoUser;
import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.OAuth2Group;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.SamlGroup;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserDirectory.QueryResult;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.atlassian.crowd.exception.OperationFailedException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserDirectoryTest
    extends AbstractComponentTest
{
  public final TestLdapServer testLdapServer1 = new TestLdapServer();

  public final TestLdapServer testLdapServer2 = new TestLdapServer();

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(tempDir) //
      .around(testLdapServer1).around(testLdapServer2);

  @Inject
  private UserDAO userDao;

  @Inject
  private LdapServerDAO ldapServerDAO;

  @Inject
  private LdapUserMappingDAO ldapUserMappingDAO;

  @Inject
  private LdapService ldapService;

  @Inject
  private UserDirectory userDirectory;

  @Inject
  private CrowdClientFactory crowdClientFactory;

  @Inject
  private SsoUserService ssoUserService;

  @Before
  public void before() {
    testLdapServer1.setWorkingDirectory(tempDir).setLdifResourceName("/UserDirectoryTest/ldap_users1.ldif");
    testLdapServer2.setWorkingDirectory(tempDir).setLdifResourceName("/UserDirectoryTest/ldap_users2.ldif");
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
  public void testGetMembersByNames() throws Exception {
    // Configure LDAP.
    configureAndStartNewLdapServer(testLdapServer1, "LDAP");
    configureAndStartNewLdapServer(testLdapServer2, "LDAP2");

    // Add a new internal user.
    tempEntity.newUser("clmbob");

    // Get users only, no groups.
    Set<String> names = Sets.newHashSet("clmbob", "testuser1", "Alpha1", "Alpha2");
    List<Member> members = userDirectory.getUsersByNames(names).get();

    assertThat(members).hasSize(2).extracting(Member::getInternalName).isSubsetOf(names);

    // Get both groups and users.
    members = userDirectory
        .getMembersByNames(Sets.newHashSet(createUser("clmbob"), createUser("testuser1"), createGroup("Alpha1"),
            createGroup("Alpha2"))).get();

    assertThat(members).hasSize(4).extracting(Member::getInternalName).containsExactlyInAnyOrderElementsOf(names);

    // Get users only, case insensitive.
    names = Sets.newHashSet("CLMBOB", "TESTUSER1", "ALPHA1", "ALPHA2");
    members = userDirectory.getUsersByNames(names).get();

    assertThat(members).hasSize(2).extracting(Member::getInternalName)
        .usingElementComparator(String.CASE_INSENSITIVE_ORDER).isSubsetOf(names);

    // Get users and groups, case insensitive.
    members = userDirectory
        .getMembersByNames(Sets.newHashSet(createUser("CLMBOB"), createUser("TESTUSER1"), createGroup("ALPHA1"),
            createGroup("ALPHA2"))).get();

    assertThat(members).hasSize(4).extracting(Member::getInternalName)
        .usingElementComparator(String.CASE_INSENSITIVE_ORDER).containsExactlyInAnyOrderElementsOf(names);

    assertThat(members.get(0).getDn()).isNull();
    assertThat(members.get(1).getDn()).matches("uid=testuser1,ou=users,dc=company,dc=com");
  }

  @Test
  public void testGetDynamicGroupsDisabled() throws Exception {
    testLdapServer1.start();

    LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    LdapConnection ldapConnection = tempEntity.newLdapConnection(ldapServer.getId(), testLdapServer1.getPort());
    LdapUserMapping ldapUserMapping = tempEntity.newLdapUserMapping(ldapServer.getId());

    ldapConnection.setSearchBase("dc=company,dc=com");
    ldapService.upsertLdapConnection(ldapConnection);

    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.DYNAMIC);
    ldapUserMapping.setDynamicGroupSearchEnabled(false);
    ldapUserMapping.setUserMemberOfGroupAttribute("departmentNumber");
    ldapUserMappingDAO.update(ldapUserMapping);
    
    assertThat(ldapService.isGroupSearchEnabled(ldapServer)).isFalse();

    QueryResult result = userDirectory.getMembersByQuery("testUsers", true);
    assertThat(result.get()).isEmpty();

    result = userDirectory.getMembersByNames(Collections.singleton(createGroup("testUsers")));
    assertThat(result.get()).hasSize(1);
    Member member = result.get().get(0);
    assertThat(member.getType()).isEqualTo(MemberType.GROUP);
    assertThat(member.getInternalName()).isEqualTo("testUsers");
    assertThat(member.getRealm()).isNull();
  }

  @Test
  public void testGetMembersByNames_WithUserDirectory_GetUsersNamingError() throws Exception {
    LdapService mockLdapService = mock(LdapService.class);
    tempEntity.newLdapServer("Test Server");
    when(mockLdapService.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapService.isGroupSearchEnabled(any(LdapServer.class))).thenReturn(true);
    Throwable namingException = new NamingException("Naming Exception!");
    when(mockLdapService.getUsersByName(any(LdapServer.class), any(String[].class))).thenThrow(namingException);

    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, mockLdapService, crowdClientFactory);

    // Add a new internal user.
    tempEntity.newUser("clmbob");

    Set<String> names = Sets.newHashSet("clmbob", "testuser1", "Alpha1");
    UserDirectory.QueryResult result = userDirectory
        .getMembersByNames(Sets.newHashSet(createUser("clmbob"), createUser("testuser1"), createGroup("Alpha1")));
    List<Member> members = result.get();

    // Verify that the internal user has been returned.
    assertThat(members).hasSize(1).extracting(Member::getInternalName).isSubsetOf(names);
    assertThat(result.getException()).isInstanceOf(NamingException.class);
    assertThat(result.getException().getSuppressed()).hasSize(1).hasOnlyElementsOfType(NamingException.class);
  }

  @Test
  public void testGetMembersByNames_WithUserDirectory_GetUsersGenericError() throws Exception {
    LdapService mockLdapService = mock(LdapService.class);
    tempEntity.newLdapServer("Test Server");
    when(mockLdapService.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapService.isGroupSearchEnabled(any(LdapServer.class))).thenReturn(true);
    Throwable exception = new RuntimeException("Exception!");
    when(mockLdapService.getUsersByName(any(LdapServer.class), any(String[].class))).thenThrow(exception);

    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, mockLdapService, crowdClientFactory);

    // Add a new internal user.
    tempEntity.newUser("clmbob");

    Set<String> names = Sets.newHashSet("clmbob", "testuser1", "Alpha1");
    UserDirectory.QueryResult result = userDirectory
        .getMembersByNames(Sets.newHashSet(createUser("clmbob"), createUser("testuser1"), createGroup("Alpha1")));
    List<Member> members = result.get();

    // Verify that the internal user has been returned.
    assertThat(members).hasSize(1).extracting(Member::getInternalName).isSubsetOf(names);
    assertThat(result.getException()).isInstanceOf(Exception.class);
    assertThat(result.getException().getSuppressed()).hasSize(1).hasOnlyElementsOfType(Exception.class);
  }

  @Test
  public void testGetMembersByNames_WithUserDirectory_GetGroupsNamingError() throws Exception {
    LdapService mockLdapService = mock(LdapService.class);
    tempEntity.newLdapServer("Test Server");
    when(mockLdapService.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapService.isGroupSearchEnabled(any(LdapServer.class))).thenReturn(true);
    Throwable namingException = new NamingException("Naming Exception!");
    when(mockLdapService.getGroupsByName(any(LdapServer.class), any(String[].class))).thenThrow(namingException);

    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, mockLdapService, crowdClientFactory);

    // Add a new internal user.
    tempEntity.newUser("clmbob");

    Set<String> names = Sets.newHashSet("clmbob", "testuser1", "Alpha1");
    UserDirectory.QueryResult result = userDirectory
        .getMembersByNames(Sets.newHashSet(createUser("clmbob"), createUser("testuser1"), createGroup("Alpha1")));
    List<Member> members = result.get();

    // Verify that the internal user has been returned.
    assertThat(members).hasSize(1).extracting(Member::getInternalName).isSubsetOf(names);
    assertThat(result.getException()).isInstanceOf(NamingException.class);
    assertThat(result.getException().getSuppressed()).containsExactly(namingException);
  }

  @Test
  public void testGetMembersByNames_WithUserDirectory_GetGroupsGenericError() throws Exception {
    LdapService mockLdapService = mock(LdapService.class);
    tempEntity.newLdapServer("Test Server");
    when(mockLdapService.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapService.isGroupSearchEnabled(any(LdapServer.class))).thenReturn(true);
    Throwable exception = new RuntimeException("Exception!");
    when(mockLdapService.getGroupsByName(any(LdapServer.class), any(String[].class))).thenThrow(exception);

    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, mockLdapService, crowdClientFactory);

    // Add a new internal user.
    tempEntity.newUser("clmbob");

    Set<String> names = Sets.newHashSet("clmbob", "testuser1", "Alpha1");
    UserDirectory.QueryResult result = userDirectory
        .getMembersByNames(Sets.newHashSet(createUser("clmbob"), createUser("testuser1"), createGroup("Alpha1")));
    List<Member> members = result.get();

    // Verify that the internal user and testuser1 have been returned.
    assertThat(members).hasSize(1).extracting(Member::getInternalName).isSubsetOf(names);
    assertThat(result.getException()).isInstanceOf(Exception.class);
    assertThat(result.getException().getSuppressed()).containsExactly(exception);
  }

  @Test
  public void testGetMembersByNames_noUnnecessaryQueries() throws Exception {
    LdapService mockLdapService = mock(LdapService.class);
    lenient().when(mockLdapService.isLdapEnabled(any(LdapServer.class))).thenReturn(true);

    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, mockLdapService, crowdClientFactory);

    UserDirectory.QueryResult result = userDirectory.getMembersByNames(new LinkedList<>());

    assertThat(result.get()).isEmpty();
    verify(mockLdapService, never()).getUsersByName(any(LdapServer.class), any(String[].class));
    verify(mockLdapService, never()).getGroupsByName(any(LdapServer.class), any(String[].class));
  }

  @Test
  public void testGetMembersByNames_WithNullOrEmptyNames() throws Exception {
    configureAndStartNewLdapServer(testLdapServer1, "LDAP");

    List<Member> members = userDirectory.getMembersByNames(new HashSet<>()).get();

    assertThat(members).isEmpty();

    members = userDirectory.getMembersByNames(null).get();

    assertThat(members).isEmpty();

    Member nullUser = new Member(MemberType.USER, null, null);
    Member nullGroup = new Member(MemberType.GROUP, null, null);
    members = userDirectory.getMembersByNames(Sets.newHashSet(nullUser, nullGroup)).get();

    assertThat(members).isEmpty();
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

    assertThat(members).extracting(Member::getInternalName).containsExactlyInAnyOrder("clmbob");

    // Get internal user case insensitive.
    members = userDirectory.getMembersByQuery("CLM BOB", false).get();

    assertThat(members).extracting(Member::getInternalName).containsExactlyInAnyOrder("clmbob");

    // Get users.
    members = userDirectory.getMembersByQuery("John Doe", true).get();

    assertThat(members).extracting(Member::getInternalName).containsExactlyInAnyOrder("testuser1", "testuser2");
    assertThat(members).extracting(Member::getDn).containsExactlyInAnyOrder("uid=testuser1,ou=users,dc=company,dc=com",
        "uid=testuser2,ou=users,dc=company,dc=com");

    // Get users, case insensitive.
    members = userDirectory.getMembersByQuery("JOHN DOE", true).get();

    assertThat(members).extracting(Member::getInternalName).containsExactlyInAnyOrder("testuser1", "testuser2");

    // Add a new internal user.
    tempEntity.newUser("alphabob", "alphaclm", "bob", "alphaclmbob@bob");
    // Get both groups and users, case insensitive.
    members = userDirectory.getMembersByQuery("ALPHA*", true).get();

    assertThat(members).extracting(Member::getInternalName).containsExactlyInAnyOrder("alphabob", "Alpha", "Alpha1",
        "Alpha2");
    assertThat(members).extracting(Member::getDn)
        .containsExactlyInAnyOrder(null, "cn=Alpha,ou=groups,dc=company,dc=com",
            "cn=Alpha1,ou=groups,dc=company,dc=com", "cn=Alpha2,ou=groups,dc=company,dc=com");
  }

  @Test
  public void testGetMembersByQuery_WithWildcards() throws Exception {
    // Configure LDAP.
    configureAndStartNewLdapServer(testLdapServer1, "LDAP");

    // Add a new internal user.
    tempEntity.newUser("clmbob", "clm", "bob", "clmbob@bob");

    // Prefix wildcard
    List<Member> members = userDirectory.getMembersByQuery("*bob", false).get();
    assertThat(members).extracting(Member::getInternalName).containsExactlyInAnyOrder("clmbob");
    members = userDirectory.getMembersByQuery("*bo", false).get();
    assertThat(members).isEmpty();

    // Suffix wildcard
    members = userDirectory.getMembersByQuery("clm*", false).get();
    assertThat(members).extracting(Member::getInternalName).containsExactlyInAnyOrder("clmbob");
    members = userDirectory.getMembersByQuery("lm*", false).get();
    assertThat(members).isEmpty();

    // Prefix and suffix wildcards
    members = userDirectory.getMembersByQuery("*lm bo*", false).get();
    assertThat(members).extracting(Member::getInternalName).containsExactlyInAnyOrder("clmbob");
    members = userDirectory.getMembersByQuery("*lmbo*", false).get();
    assertThat(members).isEmpty();
  }

  @Test
  public void testGetMembersByQuery_WithLdapError() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapService mockLdapService = mock(LdapService.class);
    when(mockLdapService.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    Throwable namingException = new NamingException("Naming Exception!");
    when(mockLdapService.findUsersByName(argThat(new SameId(ldapServer)), any(String.class), anyLong()))
        .thenThrow(namingException);

    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, mockLdapService, crowdClientFactory);

    // Add a new internal user.
    tempEntity.newUser("testclmuser", "John", "Doe", "testclmuser@testclmuser");

    UserDirectory.QueryResult result = userDirectory.getMembersByQuery("John *", false);
    List<Member> members = result.get();

    // Verify that the internal user has been returned.
    assertThat(members).extracting(Member::getInternalName).containsExactlyInAnyOrder("testclmuser");
    assertThat(result.getException()).isInstanceOf(NamingException.class);
    assertThat(result.getException().getSuppressed()).containsExactly(namingException);
  }

  @Test
  public void testGetMembersByQuery_GroupsWithLdapError() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapService mockLdapService = mock(LdapService.class);
    when(mockLdapService.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapService.isGroupSearchEnabled(argThat(new SameId(ldapServer)))).thenReturn(true);
    Throwable namingException = new NamingException("Naming Exception!");
    when(mockLdapService.findGroupsByName(argThat(new SameId(ldapServer)), any(String.class), anyLong()))
        .thenThrow(namingException);

    UserDirectory underTest =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, mockLdapService, crowdClientFactory);
    
    UserDirectory.QueryResult result = underTest.getMembersByQuery("Alpha", true);
    List<Member> members = result.get();

    assertThat(members).isEmpty();
    assertThat(result.getException()).isInstanceOf(NamingException.class);
    assertThat(result.getException().getSuppressed()).containsExactly(namingException);
  }

  @Test
  public void testGetMembersByQuery_WithGenericError() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapService mockLdapService = mock(LdapService.class);
    when(mockLdapService.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    Throwable exception = new RuntimeException("Exception!");
    when(mockLdapService.findUsersByName(argThat(new SameId(ldapServer)), any(String.class), anyLong()))
        .thenThrow(exception);

    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, mockLdapService, crowdClientFactory);

    // Add a new internal user.
    tempEntity.newUser("testclmuser", "John", "Doe", "testclmuser@testclmuser");

    UserDirectory.QueryResult result = userDirectory.getMembersByQuery("John *", false);
    List<Member> members = result.get();

    // Verify that the internal user has been returned.
    assertThat(members).extracting(Member::getInternalName).containsExactlyInAnyOrder("testclmuser");
    assertThat(result.getException()).isInstanceOf(Exception.class);
    assertThat(result.getException().getSuppressed()).containsExactly(exception);
  }

  @Test
  public void testGetMembersByQuery_GroupsWithGenericError() throws Exception {
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    LdapService mockLdapService = mock(LdapService.class);
    when(mockLdapService.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapService.isGroupSearchEnabled(argThat(new SameId(ldapServer)))).thenReturn(true);
    Throwable exception = new RuntimeException("Exception!");
    when(mockLdapService.findGroupsByName(argThat(new SameId(ldapServer)), any(String.class), anyLong()))
        .thenThrow(exception);

    UserDirectory underTest =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, mockLdapService, crowdClientFactory);

    UserDirectory.QueryResult result = underTest.getMembersByQuery("Alpha", true);
    List<Member> members = result.get();

    assertThat(members).isEmpty();
    assertThat(result.getException()).isInstanceOf(Exception.class);
    assertThat(result.getException().getSuppressed()).containsExactly(exception);
  }

  @Test
  public void testGetMembersByQuery_WithMultipleErrors() throws Exception {
    LdapServer ldapServer1 = tempEntity.newLdapServer("Test Server1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("Test Server2");
    LdapServer ldapServer3 = tempEntity.newLdapServer("Test Server3");
    LdapService mockLdapService = mock(LdapService.class);
    when(mockLdapService.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    // First LDAP server throws a generic exception
    Throwable exception = new RuntimeException("Exception!");
    when(mockLdapService.findUsersByName(argThat(new SameId(ldapServer1)), any(String.class), anyLong()))
        .thenThrow(exception);
    // Second LDAP server throws a NamingException
    Throwable namingException = new NamingException("NamingException!");
    doThrow(namingException).when(mockLdapService).findUsersByName(argThat(new SameId(ldapServer2)), any(String.class),
        anyLong());
    // Third LDAP server returns a user
    LdapUser ldapUser = new LdapUser();
    ldapUser.setServerId(ldapServer3.getId());
    ldapUser.setUsername("testldapuser");
    doReturn(Collections.singletonList(ldapUser)).when(mockLdapService)
        .findUsersByName(argThat(new SameId(ldapServer3)), any(String.class), anyLong());

    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, mockLdapService, crowdClientFactory);

    // Add a new internal user.
    tempEntity.newUser("testclmuser", "John", "Doe", "testclmuser@testclmuser");

    UserDirectory.QueryResult result = userDirectory.getMembersByQuery("John *", false);
    List<Member> members = result.get();

    // Verify that the internal user and the LDAP user have been returned.
    assertThat(members).extracting(Member::getInternalName).containsExactlyInAnyOrder("testclmuser", "testldapuser");
    assertThat(result.getException()).isInstanceOf(Exception.class);
    assertThat(result.getException().getSuppressed()).containsExactly(namingException, exception);
  }

  @Test
  public void testGetMembersByQuery_GroupsWithMultipleErrors() throws Exception {
    LdapServer ldapServer1 = tempEntity.newLdapServer("Test Server1");
    LdapServer ldapServer2 = tempEntity.newLdapServer("Test Server2");
    LdapServer ldapServer3 = tempEntity.newLdapServer("Test Server3");
    LdapService mockLdapService = mock(LdapService.class);
    when(mockLdapService.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapService.isGroupSearchEnabled(argThat(new SameId(ldapServer1)))).thenReturn(true);
    // First LDAP server throws a generic exception
    Throwable exception = new RuntimeException("Exception!");
    when(mockLdapService.findGroupsByName(argThat(new SameId(ldapServer1)), any(String.class), anyLong()))
        .thenThrow(exception);
    // Second LDAP server throws a NamingException
    doReturn(true).when(mockLdapService).isGroupSearchEnabled(argThat(new SameId(ldapServer2)));
    Throwable namingException = new NamingException("NamingException!");
    doThrow(namingException).when(mockLdapService).findGroupsByName(argThat(new SameId(ldapServer2)), any(String.class),
        anyLong());
    // Third LDAP server returns a group
    LdapGroup ldapGroup = new LdapGroup();
    ldapGroup.setGroupname("testldapgroup");
    doReturn(true).when(mockLdapService).isGroupSearchEnabled(argThat(new SameId(ldapServer3)));
    doReturn(Collections.singletonList(ldapGroup)).when(mockLdapService)
        .findGroupsByName(argThat(new SameId(ldapServer3)), any(String.class), anyLong());

    UserDirectory underTest =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, mockLdapService, crowdClientFactory);
    
    UserDirectory.QueryResult result = underTest.getMembersByQuery("any", true);
    List<Member> members = result.get();

    // Verify that the LDAP group have been returned.
    assertThat(members).hasSize(1);
    assertThat(members.get(0).getDisplayName()).isEqualTo("testldapgroup");
    assertThat(result.getException()).isInstanceOf(Exception.class);
    assertThat(result.getException().getSuppressed()).containsExactly(namingException, exception);
  }

  @Test
  public void testGetUsersByRealNames_MatchesAgainstInternalUsersAndAllConfiguredLDAPServersCombiningResults()
      throws Exception
  {
    configureAndStartNewLdapServer(testLdapServer1, "LDAP1");
    configureAndStartNewLdapServer(testLdapServer2, "LDAP2");

    tempEntity.newUser(
        "internal-user-1", "internal", "users-1", "internal-user1@example.com");
    tempEntity.newUser(
        "internal-user-2", "internal", "users-2", "internal-user2@example.com");
    tempEntity.newUser(
        "internal-user-3", "internal", "users-3", "internal-user3@example.com");

    final List<Member> members = userDirectory.getUsersByRealNames(Sets.newHashSet(
        "internal users-2",
        "internal users-1",
        "John Doe", // ldap1 user
        "Jannet Ray", // ldap2 user
        "Nobody Here"
    ));

    // should return a list of any users that matched internally or in ldap and exclude any users that did not match
    assertThat(members).extracting(Member::getInternalName).containsExactly(
        "internal-user-1", "internal-user-2", "testuser1", "testuser3");
    assertThat(members).extracting(Member::getRealm).containsExactly(
        "IQ Server", "IQ Server", "LDAP1", "LDAP2"
    );
  }

  @Test
  public void testGetUsersByRealNames_OnlyQueriesLdapForUsersNotFoundInternally() throws NamingException {
    final LdapService mockLdapService = mockEnabledLdapService("Test Server");

    when(mockLdapService.getUsersByRealName(any(), any())).thenReturn(Lists.newArrayList());

    tempEntity.newUser(
        "internal-user-1", "internal", "users-1", "internal-user1@example.com");
    tempEntity.newUser(
        "internal-user-2", "internal", "users-2", "internal-user2@example.com");

    final UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, mockLdapService, crowdClientFactory);

    userDirectory.getUsersByRealNames(Sets.newHashSet(
        "internal users-2",
        "internal users-1",
        "Sam Jenkins",
        "Jim Varney"
    ));

    // these are the arguments that didn't already match to an internal user vis userDao
    final String[] expectedLdapArguments = { "Jim Varney", "Sam Jenkins" };

    // should only have tried to find ldap users if they were not found as internal users
    verify(mockLdapService).getUsersByRealName(any(LdapServer.class), eq(expectedLdapArguments));
  }

  @Test
  public void testGetUsersByRealNames_HandlesNullAndEmptyLists() {
    final var resultsForNull = userDirectory.getUsersByRealNames(null);
    assertThat(resultsForNull).isEmpty();

    final var resultsForEmpty = userDirectory.getUsersByRealNames(Sets.newHashSet());
    assertThat(resultsForEmpty).isEmpty();
  }

  @Test
  public void testGetUsersByEmails_MatchesAgainstAllConfiguredRealms_CombiningResults_OAuth2()
      throws Exception
  {
    enableSsoWithOAuth2();
    OAuth2User ssoUser = tempEntity.newOAuth2User("oauth2user", null, null, "oauth2user@example.com", null);
    testGetUsersByEmails_MatchesAgainstAllConfiguredRealms_CombiningResults(ssoUser);
  }

  @Test
  public void testGetUsersByEmails_MatchesAgainstAllConfiguredRealms_CombiningResults_Saml() throws Exception {
    enableSsoWithSaml();
    SamlUser ssoUser = tempEntity.newSamlUser("samluser", null, null, "samluser@example.com", null);
    testGetUsersByEmails_MatchesAgainstAllConfiguredRealms_CombiningResults(ssoUser);
  }

  private void testGetUsersByEmails_MatchesAgainstAllConfiguredRealms_CombiningResults(AbstractSsoUser ssoUser)
      throws Exception
  {
    configureAndStartNewLdapServer(testLdapServer1, "LDAP1");
    configureAndStartNewLdapServer(testLdapServer2, "LDAP2");

    tempEntity.newUser(
        "internal-user-1", "internal", "users-1", "internal-user1@example.com");
    tempEntity.newUser(
        "internal-user-2", "internal", "users-2", "internal-user2@example.com");
    tempEntity.newUser(
        "internal-user-3", "internal", "users-3", "internal-user3@example.com");

    final List<Member> members = userDirectory.getUsersByEmails(Sets.newHashSet(
        "internal-user2@example.com",
        "internal-user1@example.com",
        "test.user@company.com", // ldap1 user
        "test.user2@company.com", // ldap2 user
        ssoUser.getEmail(),
        "not-in-any-user-provider@example.com"
    ));

    // should return a list of any users that matched internally or in ldap and exclude any users that did not match
    assertThat(members).extracting(Member::getInternalName).containsExactly(
        "internal-user-1", "internal-user-2", "testuser1", "testuser2", ssoUser.getUsername());
    assertThat(members).extracting(Member::getRealm).containsExactly(
        "IQ Server", "IQ Server", "LDAP1", "LDAP2", ssoUser.getRealmId()
    );
  }

  @Test
  public void testGetUsersByEmails_OnlyQueriesLdapForUsersNotFoundInternally() throws NamingException {
    final LdapService mockLdapService = mockEnabledLdapService("Test Server");

    when(mockLdapService.getUsersByEmail(any(), any())).thenReturn(Lists.newArrayList());

    tempEntity.newUser(
        "internal-user-1", "internal", "users-1", "internal-user1@example.com");
    tempEntity.newUser(
        "internal-user-2", "internal", "users-2", "internal-user2@example.com");

    final UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, mockLdapService, crowdClientFactory);

    userDirectory.getUsersByEmails(Sets.newHashSet(
        "internal-user2@example.com",
        "internal-user1@example.com",
        "ldap1@gmail.com",
        "ldap2@gmail.com"
    ));

    // these are the arguments that didn't already match to an internal user via userDao
    final String[] expectedLdapArguments = { "ldap1@gmail.com", "ldap2@gmail.com" };

    // should only have tried to find ldap users if they were not found as internal users
    verify(mockLdapService).getUsersByEmail(any(LdapServer.class), eq(expectedLdapArguments));
  }

  @Test
  public void testGetUsersByEmails_HandlesNullAndEmptyLists() {
    final var resultsForNull = userDirectory.getUsersByEmails(null);
    assertThat(resultsForNull).isEmpty();

    final var resultsForEmpty = userDirectory.getUsersByEmails(Sets.newHashSet());
    assertThat(resultsForEmpty).isEmpty();
  }

  @Test
  public void testGetUsersByNames_LdapOnlyCalledWithNamesNotFoundInInternalRealm() throws Exception {
    LdapService mockLdapService = mockEnabledLdapService("Test Server");

    List<LdapUser> emptyLdapUsers = new ArrayList<>();
    String[] expectedArgument = new String[] { "Alpha", "CLMBOB" };
    when(mockLdapService.getUsersByName(any(LdapServer.class), eq(expectedArgument)))
        .thenReturn(emptyLdapUsers);

    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, mockLdapService, crowdClientFactory);

    // Add a new internal user.
    tempEntity.newUser("testclmuser", "John", "Doe", "testclmuser@testclmuser");

    UserDirectory.QueryResult result = userDirectory.getUsersByNames(Sets.newHashSet("tesTcLmUsEr", expectedArgument[0],
        expectedArgument[1]));
    List<Member> members = result.get();

    // Verify that only the internal user has been returned.
    assertThat(members).extracting(Member::getInternalName).containsExactlyInAnyOrder("testclmuser");
    // That 'John' was removed from the user names to search.
    verify(mockLdapService).getUsersByName(any(LdapServer.class), eq(expectedArgument));

    // Test that the get users method isn't called when only internal users are provided.
    userDirectory.getUsersByNames(Sets.newHashSet("tesTcLmUsEr"));
    // Count of the number of calls is still one, as expected.
    verify(mockLdapService, times(1)).getUsersByName(any(LdapServer.class), any(String[].class));
  }

  @Test
  public void testGetUsersByNames_MultipleLdapServers() throws Exception {
    // Configure LDAP.
    configureAndStartNewLdapServer(testLdapServer1, "LDAP1");
    configureAndStartNewLdapServer(testLdapServer2, "LDAP2");

    // Get one user
    List<Member> members = userDirectory.getUsersByNames(Sets.newHashSet("testuser1")).get();
    assertThat(members).hasSize(1);
    assertThat(members.get(0).getInternalName()).isEqualTo("testuser1");
    assertThat(members.get(0).getRealm()).isEqualTo("LDAP1");

    // Get users from both server 1 and server 2
    members = userDirectory.getUsersByNames(Sets.newHashSet("testuser1", "testuser2")).get();
    assertThat(members).hasSize(2);
    assertThat(members.get(0).getInternalName()).isEqualTo("testuser1");
    assertThat(members.get(0).getRealm()).isEqualTo("LDAP1");
    assertThat(members.get(0).getDn()).isEqualTo("uid=testuser1,ou=users,dc=company,dc=com");
    assertThat(members.get(1).getInternalName()).isEqualTo("testuser2");
    assertThat(members.get(1).getRealm()).isEqualTo("LDAP2");
    assertThat(members.get(1).getDn()).isEqualTo("uid=testuser2,ou=users,dc=company,dc=com");
  }

  @Test
  public void testGetMembersByQuery_WithNullOrEmptyQuery() throws Exception {
    configureAndStartNewLdapServer(testLdapServer1, "LDAP");

    List<Member> members = userDirectory.getMembersByQuery(null, false).get();

    assertThat(members).isEmpty();

    members = userDirectory.getMembersByQuery("", true).get();

    assertThat(members).isEmpty();
  }

  @Test
  public void testValidateUsers_NullSet() {
    Set<String> invalidUsers = userDirectory.validateUsers(null);
    assertThat(invalidUsers).isEmpty();
  }

  @Test
  public void testValidateUsers_EmptySet() {
    Set<String> users = Collections.emptySet();
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers).isEmpty();
  }

  @Test
  public void testValidateUsers_InternalUserFound() {
    User testUser = tempEntity.newUser();

    Set<String> users = Collections.singleton(testUser.getUsername());
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers).isEmpty();
  }

  @Test
  public void testValidateUsers_Case_Insensitive() {
    User testUser = tempEntity.newUser("TestUser1");

    // Test with a found user.
    Set<String> users = Collections.singleton(testUser.getUsername().toLowerCase(Locale.ENGLISH));
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers).isEmpty();

    // Test with invalid users.
    users = Sets.newHashSet("Bob", "Sue", "Mary");
    invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers).hasSize(3);

    // Ensure that the names returned match the users input.
    assertThat(invalidUsers).isEqualTo(users);
  }

  @Test
  public void testValidateUsers_LdapUserFound() throws Exception {
    configureAndStartNewLdapServer(testLdapServer1, "LDAP");

    Set<String> users = Sets.newHashSet("testuser1");
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers).isEmpty();
  }

  @Test
  public void testValidateUsers_InternalUserNotFound_LdapNotConfigured() {
    Set<String> users = Sets.newHashSet("invaliduser");
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers).containsExactlyInAnyOrder("invaliduser");
  }

  @Test
  public void testValidateUsers_InternalUserNotFound_LdapConfigured() throws Exception {
    configureAndStartNewLdapServer(testLdapServer1, "LDAP");

    Set<String> users = Sets.newHashSet("invaliduser");
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers).containsExactlyInAnyOrder("invaliduser");
  }

  @Test
  public void testValidateUsers_LdapErrorOnGetUsers() throws Exception {
    LdapService mockLdapService = mock(LdapService.class);
    LdapServer ldapServer = tempEntity.newLdapServer("Test Server");
    when(mockLdapService.isLdapEnabled(any(LdapServer.class))).thenReturn(true);
    when(mockLdapService.getUsersByName(argThat(new SameId(ldapServer)), any(String[].class)))
        .thenThrow(new NamingException("Naming Exception!"));

    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, mockLdapService, crowdClientFactory);

    Set<String> users = Sets.newHashSet("invaliduser");
    Set<String> invalidUsers = userDirectory.validateUsers(users);
    assertThat(invalidUsers).containsExactlyInAnyOrder("invaliduser");
  }

  @Test
  public void testIsLdapUser() throws Exception {
    configureAndStartNewLdapServer(testLdapServer1, "LDAP");
    configureAndStartNewLdapServer(testLdapServer2, "LDAP2");

    assertThat(userDirectory.isLdapUser(new User("testuser1", null, null, null, null))).isTrue();
    assertThat(userDirectory.isLdapUser(new User("testuser2", null, null, null, null))).isTrue();
    assertThat(userDirectory.isLdapUser(new User("not-a-real-user", null, null, null, null))).isFalse();
  }

  private static Member createUser(String name) {
    return new Member(MemberType.USER, name, null, null, null, null);
  }

  private static Member createGroup(String name) {
    return new Member(MemberType.GROUP, name, null);
  }

  @Test
  public void testGetMembersByQuery_AuthenticatedUsersGroup_GroupsDisabled() {
    List<Member> members = userDirectory.getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME, false).get();
    assertThat(members).isEmpty();

    members = userDirectory.getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME + "*", false).get();
    assertThat(members).isEmpty();
  }

  @Test
  public void testGetMembersByQuery_AuthenticatedUsersGroup_GroupsEnabled() {
    // Exact name
    List<Member> members = userDirectory.getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME, true).get();
    assertThat(members).hasSize(1);
    assertIsAuthenticatedUsersGroup(members.get(0));

    // With wild card
    members = userDirectory.getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME + "*", true).get();
    assertThat(members).hasSize(1);
    assertIsAuthenticatedUsersGroup(members.get(0));
    members = userDirectory.getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME.substring(0, 5) + "*", true)
        .get();
    assertThat(members).hasSize(1);
    assertIsAuthenticatedUsersGroup(members.get(0));

    // With wild card and special regex chars - should not throw an exception because the regex pattern is incorrect.
    members = userDirectory
        .getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME.substring(0, 5) + "(*", true).get();
    assertThat(members).isEmpty();

    // Case insensitive
    members = userDirectory.getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME.toLowerCase(Locale.ENGLISH),
        true).get();
    assertThat(members).hasSize(1);
    assertIsAuthenticatedUsersGroup(members.get(0));
    members = userDirectory.getMembersByQuery(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME.toUpperCase(Locale.ENGLISH),
        true).get();
    assertThat(members).hasSize(1);
    assertIsAuthenticatedUsersGroup(members.get(0));
  }

  private void assertIsAuthenticatedUsersGroup(Member member) {
    assertThat(member.getType()).isEqualTo(MemberType.GROUP);
    assertThat(member.getDisplayName()).isEqualTo(Group.AUTHENTICATED_USERS_GROUP_DISPLAY_NAME);
    assertThat(member.getInternalName()).isEqualTo(Group.AUTHENTICATED_USERS_GROUP_ID);
    assertThat(member.getInternalNameLowerCase())
        .isEqualTo(Group.AUTHENTICATED_USERS_GROUP_ID.toLowerCase(Locale.ENGLISH));
    assertThat(member.getEmail()).isNull();
    assertThat(member.getRealm()).isEqualTo(InternalRealm.DISPLAY_NAME);
  }

  @Test
  public void testGetMembersByNames_AuthenticatedUsersGroup() {
    // Exact name
    List<Member> members = userDirectory.getMembersByNames(
        Collections.singleton(createGroup(Group.AUTHENTICATED_USERS_GROUP_ID))).get();
    assertThat(members).hasSize(1);
    assertIsAuthenticatedUsersGroup(members.get(0));

    // Case insensitive
    members = userDirectory.getMembersByNames(
        Collections.singleton(createGroup(Group.AUTHENTICATED_USERS_GROUP_ID.toLowerCase(Locale.ENGLISH)))).get();
    assertThat(members).hasSize(1);
    assertIsAuthenticatedUsersGroup(members.get(0));
    members = userDirectory.getMembersByNames(
        Collections.singleton(createGroup(Group.AUTHENTICATED_USERS_GROUP_ID.toUpperCase(Locale.ENGLISH)))).get();
    assertThat(members).hasSize(1);
    assertIsAuthenticatedUsersGroup(members.get(0));
  }

  @Test
  public void testGetMembersByQuery_SameUserInMultipleRealms() throws Exception {
    configureAndStartNewLdapServer(testLdapServer1, "LDAP1");

    // Should get back only the user from testLdapServer1.
    List<Member> members = userDirectory.getMembersByQuery("Beta User", false).get();
    assertThat(members).hasSize(1);
    assertThat(members.get(0).getDisplayName()).isEqualTo("Beta User");
    assertThat(members.get(0).getRealm()).isEqualTo("LDAP1");
    assertThat(members.get(0).getDn()).isEqualTo("uid=Beta,ou=users,dc=company,dc=com");

    // Start testLdapServer2. Should still get back only the user from testLdapServer1 since it is higher priority.
    configureAndStartNewLdapServer(testLdapServer2, "LDAP2");
    members = userDirectory.getMembersByQuery("Beta User", false).get();
    assertThat(members).hasSize(1);
    assertThat(members.get(0).getDisplayName()).isEqualTo("Beta User");
    assertThat(members.get(0).getRealm()).isEqualTo("LDAP1");
    assertThat(members.get(0).getDn()).isEqualTo("uid=Beta,ou=users,dc=company,dc=com");
  
    // Add a new IQ user. Should get back only the IQ user.
    tempEntity.newUser("beta", "Beta", "User", "betauser@example.com");
    members = userDirectory.getMembersByQuery("Beta User", false).get();
    assertThat(members).hasSize(1);
    assertThat(members.get(0).getDisplayName()).isEqualTo("Beta User");
    assertThat(members.get(0).getRealm()).isEqualTo("IQ Server");
    assertThat(members.get(0).getDn()).isNull();
  }

  @Test
  public void testGetMembersByQuery_SameGroupInMultipleRealms() throws Exception {
    configureAndStartNewLdapServer(testLdapServer1, "LDAP1");
    configureAndStartNewLdapServer(testLdapServer2, "LDAP2");

    // Should return all groups from all realms. When same group occurs in both realms a single occurrence is retrieved.
    List<Member> members = userDirectory.getMembersByQuery("Alpha*", true).get();
    assertThat(members).extracting(Member::getDisplayName).containsExactlyInAnyOrder("Alpha", "Alpha1", "Alpha2");
  }

  @Test
  public void testGetUsersByNames_Crowd_EmptyAfterOtherRealms() {
    CrowdClientFactory mockCrowdClientFactory = mock(CrowdClientFactory.class);
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, mockCrowdClientFactory);

    // Fetch the actual admin user to get its ID
    User adminUser = userDao.getByUsernameNotNull("admin");
    QueryResult result = userDirectory.getUsersByNames(Sets.newHashSet("admin"));

    assertThat(result).isNotNull();
    assertThat(result.get()).usingRecursiveFieldByFieldElementComparator().containsExactly(
        new Member(MemberType.USER, "admin", "Admin BuiltIn", "admin@localhost",
                InternalRealm.DISPLAY_NAME, adminUser.getId()));
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
    verify(mockCrowdClientFactory, never()).createCrowdClient();
  }

  @Test
  public void testGetUsersByNames_Crowd_NullCrowdClient() {
    QueryResult result = userDirectory.getUsersByNames(Sets.newHashSet("username"));

    assertThat(result).isNotNull();
    assertThat(result.get()).isEmpty();
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetUsersByNames_Crowd_Exception() throws Exception {
    CrowdClientFactory mockCrowdClientFactory = mock(CrowdClientFactory.class);
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, mockCrowdClientFactory);
    CrowdClient mockCrowdClient = mock(CrowdClient.class);
    OperationFailedException operationFailedException = new OperationFailedException();
    when(mockCrowdClient.searchUsersByUsernames(any())).thenThrow(operationFailedException);
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);
    Set<String> usernames = Sets.newHashSet("username1", "username2", "username3");

    QueryResult result = userDirectory.getUsersByNames(usernames);

    verify(mockCrowdClient).searchUsersByUsernames(ArgumentMatchers.eq(usernames));
    assertThat(result).isNotNull();
    assertThat(result.get()).isEmpty();
    assertThat(result.hasException()).isTrue();
    assertThat(result.getException()).isInstanceOf(Exception.class).hasSuppressedException(operationFailedException);
  }

  @Test
  public void testGetUsersByNames_Crowd_NoResults() throws Exception {
    CrowdClientFactory mockCrowdClientFactory = mock(CrowdClientFactory.class);
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, mockCrowdClientFactory);
    CrowdClient mockCrowdClient = mock(CrowdClient.class);
    when(mockCrowdClient.searchUsersByUsernames(any())).thenReturn(Collections.emptySet());
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);
    Set<String> usernames = Sets.newHashSet("username1", "username2", "username3");

    QueryResult result = userDirectory.getUsersByNames(usernames);

    verify(mockCrowdClient).searchUsersByUsernames(ArgumentMatchers.eq(usernames));
    assertThat(result).isNotNull();
    assertThat(result.get()).isEmpty();
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetUsersByNames_Crowd() throws Exception {
    CrowdClientFactory mockCrowdClientFactory = mock(CrowdClientFactory.class);
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, mockCrowdClientFactory);
    CrowdClient mockCrowdClient = mock(CrowdClient.class);
    List<Member> members = Arrays.asList(
        new Member(MemberType.USER, "username1", "displayName1", "email1", CrowdRealm.ID, null),
        new Member(MemberType.USER, "username2", "displayName2", "email2", CrowdRealm.ID, null)
    );
    Set<String> queriedUsernames = new LinkedHashSet<>();
    when(mockCrowdClient.searchUsersByUsernames(any())).thenAnswer(invocationOnMock -> {
      queriedUsernames.addAll(invocationOnMock.getArgument(0));
      return new LinkedHashSet<>(members);
    });
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);
    Set<String> usernames = Sets.newHashSet("username1", "username2", "username3");

    QueryResult result = userDirectory.getUsersByNames(usernames);

    verify(mockCrowdClient).searchUsersByUsernames(
        ArgumentMatchers.eq(Sets.newLinkedHashSet(Collections.singletonList("username3"))));
    assertThat(result).isNotNull();
    assertThat(result.get()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(members.toArray(new Member[0]));
    assertThat(queriedUsernames).isEqualTo(usernames);
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetUsersByNames_Saml() {
    // SAML not configured
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    SamlUser samlUser1 = tempEntity.newSamlUser("username1", null, null, null, null);
    SamlUser samlUser2 = tempEntity.newSamlUser("username2", null, null, null, null);
    tempEntity.newSamlUser("username3", null, null, null, null);
    Set<String> usernames = Sets.newHashSet("username1", "username2", "username4");

    QueryResult result = userDirectory.getUsersByNames(usernames);
    assertThat(result).isNotNull();
    assertThat(result.get()).isEmpty();
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();

    // Configure SAML
    enableSsoWithSaml();

    result = userDirectory.getUsersByNames(usernames);

    result = userDirectory.getUsersByNames(usernames);

    assertThat(result).isNotNull();
    assertThat(result.get()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(
            new Member(MemberType.USER, samlUser1.getUsername(), samlUser1.calculateDisplayName(), samlUser1.getEmail(),
                SamlRealm.ID, samlUser1.getId()),
            new Member(MemberType.USER, samlUser2.getUsername(), samlUser2.calculateDisplayName(), samlUser2.getEmail(),
                SamlRealm.ID, samlUser2.getId()));
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetMembersByQuery_Users_Saml_NotConfigured() {
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    tempEntity.newSamlUser("username1", null, null, null, null);
    tempEntity.newSamlUser("username2", null, null, null, null);

    QueryResult result = userDirectory.getMembersByQuery("username*", false);

    assertThat(result).isNotNull();
    assertThat(result.get()).isEmpty();
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetMembersByQuery_Users_Saml() {
    enableSsoWithSaml();

    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    SamlUser samlUser1 = tempEntity.newSamlUser("username1", "bob", "smith", null, null);
    SamlUser samlUser2 = tempEntity.newSamlUser("username2", "john", "smith", null, null);

    QueryResult result = userDirectory.getMembersByQuery("*smith", false);

    assertThat(result).isNotNull();
    assertThat(result.get()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(
            new Member(MemberType.USER, samlUser1.getUsername(), samlUser1.calculateDisplayName(), samlUser1.getEmail(),
                SamlRealm.ID, samlUser1.getId()),
            new Member(MemberType.USER, samlUser2.getUsername(), samlUser2.calculateDisplayName(), samlUser2.getEmail(),
                SamlRealm.ID, samlUser2.getId()));
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetMembersByQuery_Groups_GroupSearchEnabled_Saml_NotConfigured() {
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    tempEntity.newSamlGroup("group1");
    tempEntity.newSamlGroup("group2");

    QueryResult result = userDirectory.getMembersByQuery("group*", true);

    assertThat(result.get()).isEmpty();
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetMembersByQuery_Groups_GroupSearchEnabled_Saml() {
    enableSsoWithSaml();

    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");

    QueryResult result = userDirectory.getMembersByQuery("group*", true);

    assertThat(result.get()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(
            new Member(MemberType.GROUP, samlGroup1.getName(), samlGroup1.getName(), null, SamlRealm.ID),
            new Member(MemberType.GROUP, samlGroup2.getName(), samlGroup2.getName(), null, SamlRealm.ID));
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetMembersByQuery_Groups_GroupSearchDisabled_Saml() {
    tempEntity.newSamlConfiguration();
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    tempEntity.newSamlGroup("group1");
    tempEntity.newSamlGroup("group2");

    QueryResult result = userDirectory.getMembersByQuery("group*", false);

    assertThat(result.get()).isEmpty();
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetUsersByNames_OAuth2() {
    // OAuth2 not configured
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    OAuth2User oauth2User1 = tempEntity.newOAuth2User("username1", null, null, null, null);
    OAuth2User oauth2User2 = tempEntity.newOAuth2User("username2", null, null, null, null);
    tempEntity.newOAuth2User("username3", null, null, null, null);
    Set<String> usernames = Sets.newHashSet("username1", "username2", "username4");

    QueryResult result = userDirectory.getUsersByNames(usernames);

    assertThat(result).isNotNull();
    assertThat(result.get()).isEmpty();
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();

    // Configure OAuth2
    enableSsoWithOAuth2();

    result = userDirectory.getUsersByNames(usernames);

    assertThat(result).isNotNull();
    assertThat(result.get()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(
            new Member(MemberType.USER, oauth2User1.getUsername(), oauth2User1.calculateDisplayName(),
                oauth2User1.getEmail(),
                OAuth2Realm.ID, oauth2User1.getId()),
            new Member(MemberType.USER, oauth2User2.getUsername(), oauth2User2.calculateDisplayName(),
                oauth2User2.getEmail(),
                OAuth2Realm.ID, oauth2User2.getId()));
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetMembersByQuery_Users_OAuth2_NotConfigured() {
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    tempEntity.newOAuth2User("username1", null, null, null, null);
    tempEntity.newOAuth2User("username2", null, null, null, null);

    QueryResult result = userDirectory.getMembersByQuery("username*", false);

    assertThat(result).isNotNull();
    assertThat(result.get()).isEmpty();
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetMembersByQuery_Users_OAuth2() {
    enableSsoWithOAuth2();

    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    OAuth2User oauth2User1 = tempEntity.newOAuth2User("username1", "bob", "smith", null, null);
    OAuth2User oauth2User2 = tempEntity.newOAuth2User("username2", "john", "smith", null, null);

    QueryResult result = userDirectory.getMembersByQuery("*smith", false);

    assertThat(result).isNotNull();
    assertThat(result.get()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(
            new Member(MemberType.USER, oauth2User1.getUsername(), oauth2User1.calculateDisplayName(),
                oauth2User1.getEmail(),
                OAuth2Realm.ID, oauth2User1.getId()),
            new Member(MemberType.USER, oauth2User2.getUsername(), oauth2User2.calculateDisplayName(),
                oauth2User2.getEmail(),
                OAuth2Realm.ID, oauth2User2.getId()));
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetMembersByQuery_Groups_GroupSearchEnabled_OAuth2_NotConfigured() {
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    tempEntity.newOAuth2Group("group1");
    tempEntity.newOAuth2Group("group2");

    QueryResult result = userDirectory.getMembersByQuery("group*", true);

    assertThat(result.get()).isEmpty();
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetMembersByQuery_Groups_GroupSearchEnabled_OAuth2() {
    enableSsoWithOAuth2();

    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    OAuth2Group oauth2Group1 = tempEntity.newOAuth2Group("group1");
    OAuth2Group oauth2Group2 = tempEntity.newOAuth2Group("group2");

    QueryResult result = userDirectory.getMembersByQuery("group*", true);

    assertThat(result.get()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(
            new Member(MemberType.GROUP, oauth2Group1.getName(), oauth2Group1.getName(), null,
                OAuth2Realm.ID),
            new Member(MemberType.GROUP, oauth2Group2.getName(), oauth2Group2.getName(), null,
                OAuth2Realm.ID));
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetMembersByQuery_Groups_GroupSearchDisabled_OAuth2() {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    tempEntity.newOAuth2Configuration();

    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    tempEntity.newOAuth2Group("group1");
    tempEntity.newOAuth2Group("group2");

    QueryResult result = userDirectory.getMembersByQuery("group*", false);

    assertThat(result.get()).isEmpty();
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetMembersByNames_Crowd_EmptyAfterOtherRealms() throws Exception {
    CrowdClientFactory mockCrowdClientFactory = mock(CrowdClientFactory.class);
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, mockCrowdClientFactory);
    configureAndStartNewLdapServer(testLdapServer1, "LDAP");
    
    QueryResult result = userDirectory.getMembersByNames(Sets.newHashSet(createGroup("Alpha1")));

    assertThat(result).isNotNull();
    Member expectedMember = new Member(MemberType.GROUP, "Alpha1", "Alpha1", null, "LDAP");
    expectedMember.setDn("cn=Alpha1,ou=groups,dc=company,dc=com");
    assertThat(result.get()).usingRecursiveFieldByFieldElementComparator().containsExactly(expectedMember);
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
    verify(mockCrowdClientFactory, never()).createCrowdClient();
  }

  @Test
  public void testGetMembersByNames_Crowd_NullCrowdClient() {
    Set<Member> members = new LinkedHashSet<>(Arrays.asList(
        new Member(MemberType.GROUP, "group1", "group1", null, CrowdRealm.ID),
        new Member(MemberType.GROUP, "group2", "group2", null, CrowdRealm.ID),
        new Member(MemberType.GROUP, "group3", "group3", null, CrowdRealm.ID)
    ));

    QueryResult result = userDirectory.getMembersByNames(members);

    assertThat(result).isNotNull();
    assertThat(result.get()).isEmpty();
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetMembersByNames_Crowd_Exception() throws Exception {
    CrowdClientFactory mockCrowdClientFactory = mock(CrowdClientFactory.class);
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, mockCrowdClientFactory);
    CrowdClient mockCrowdClient = mock(CrowdClient.class);
    OperationFailedException operationFailedException = new OperationFailedException();
    when(mockCrowdClient.searchGroupsByGroupNames(any())).thenThrow(operationFailedException);
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);
    Set<Member> members = new LinkedHashSet<>(Arrays.asList(
        new Member(MemberType.GROUP, "group1", "group1", null, CrowdRealm.ID),
        new Member(MemberType.GROUP, "group2", "group2", null, CrowdRealm.ID),
        new Member(MemberType.GROUP, "group3", "group3", null, CrowdRealm.ID)
    ));

    QueryResult result = userDirectory.getMembersByNames(members);

    assertThat(result).isNotNull();
    assertThat(result.get()).isEmpty();
    assertThat(result.hasException()).isTrue();
    assertThat(result.getException()).isInstanceOf(Exception.class).hasSuppressedException(operationFailedException);
  }

  @Test
  public void testGetMembersByNames_Crowd_NoResults() throws Exception {
    CrowdClientFactory mockCrowdClientFactory = mock(CrowdClientFactory.class);
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, mockCrowdClientFactory);
    CrowdClient mockCrowdClient = mock(CrowdClient.class);
    when(mockCrowdClient.searchGroupsByGroupNames(any())).thenReturn(Collections.emptySet());
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);
    Set<Member> members = new LinkedHashSet<>(Arrays.asList(
        new Member(MemberType.GROUP, "group1", "group1", null, CrowdRealm.ID),
        new Member(MemberType.GROUP, "group2", "group2", null, CrowdRealm.ID),
        new Member(MemberType.GROUP, "group3", "group3", null, CrowdRealm.ID)
    ));

    QueryResult result = userDirectory.getMembersByNames(members);

    verify(mockCrowdClient).searchGroupsByGroupNames(ArgumentMatchers.eq(
        members.stream().map(Member::getInternalName).collect(Collectors.toCollection(LinkedHashSet::new))));
    assertThat(result).isNotNull();
    assertThat(result.get()).isEmpty();
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }
  
  @Test
  public void testGetMembersByNames_Crowd() throws Exception {
    CrowdClientFactory mockCrowdClientFactory = mock(CrowdClientFactory.class);
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, mockCrowdClientFactory);
    CrowdClient mockCrowdClient = mock(CrowdClient.class);
    List<Member> expectedMembers = Arrays.asList(
        new Member(MemberType.GROUP, "group1", "group1", null, CrowdRealm.ID),
        new Member(MemberType.GROUP, "group2", "group2", null, CrowdRealm.ID)
    );
    Set<String> queriedGroupNames = new LinkedHashSet<>();
    when(mockCrowdClient.searchGroupsByGroupNames(any())).thenAnswer(invocationOnMock -> {
      queriedGroupNames.addAll(invocationOnMock.getArgument(0));
      return new LinkedHashSet<>(expectedMembers);
    });
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);
    Set<Member> members = new LinkedHashSet<>(Arrays.asList(
        new Member(MemberType.GROUP, "group1", "group1", null, CrowdRealm.ID),
        new Member(MemberType.GROUP, "group2", "group2", null, CrowdRealm.ID),
        new Member(MemberType.GROUP, "group3", "group3", null, CrowdRealm.ID)
    ));

    QueryResult result = userDirectory.getMembersByNames(members);

    verify(mockCrowdClient).searchGroupsByGroupNames(
        ArgumentMatchers.eq(Sets.newLinkedHashSet(Collections.singletonList("group3"))));
    assertThat(result).isNotNull();
    assertThat(result.get()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(expectedMembers.toArray(new Member[0]));
    assertThat(queriedGroupNames).isEqualTo(
        members.stream().map(Member::getInternalName).collect(Collectors.toCollection(LinkedHashSet::new)));
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetMembersByQuery_Users_Crowd() throws Exception {
    CrowdClientFactory mockCrowdClientFactory = mock(CrowdClientFactory.class);
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, mockCrowdClientFactory);
    CrowdClient mockCrowdClient = mock(CrowdClient.class);
    List<Member> expectedMembers = Arrays.asList(
        new Member(MemberType.USER, "username1", "displayName1", "email1", CrowdRealm.ID, null),
        new Member(MemberType.USER, "username2", "displayName2", "email2", CrowdRealm.ID, null)
    );
    when(mockCrowdClient.searchUsersByDisplayName(eq("displayName*"))).thenReturn(new LinkedHashSet<>(expectedMembers));
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);

    QueryResult result = userDirectory.getMembersByQuery("displayName*", false);

    assertThat(result.get()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(expectedMembers.toArray(new Member[0]));
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetMembersByQuery_Groups_GroupSearchEnabled_Crowd() throws Exception {
    CrowdClientFactory mockCrowdClientFactory = mock(CrowdClientFactory.class);
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, mockCrowdClientFactory);
    CrowdClient mockCrowdClient = mock(CrowdClient.class);
    when(mockCrowdClient.searchUsersByDisplayName(any())).thenReturn(Collections.emptySet());
    List<Member> expectedMembers = Arrays.asList(
        new Member(MemberType.GROUP, "group1", "group1", null, CrowdRealm.ID),
        new Member(MemberType.GROUP, "group2", "group2", null, CrowdRealm.ID)
    );
    when(mockCrowdClient.searchGroupsByGroupNames(eq(Collections.singleton("group*")))).thenReturn(
        new LinkedHashSet<>(expectedMembers));
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);

    QueryResult result = userDirectory.getMembersByQuery("group*", true);

    assertThat(result.get()).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(expectedMembers.toArray(new Member[0]));
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
  }

  @Test
  public void testGetMembersByQuery_Groups_GroupSearchDisabled_Crowd() throws Exception {
    CrowdClientFactory mockCrowdClientFactory = mock(CrowdClientFactory.class);
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, mockCrowdClientFactory);
    CrowdClient mockCrowdClient = mock(CrowdClient.class);
    when(mockCrowdClient.searchUsersByDisplayName(any())).thenReturn(Collections.emptySet());
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);

    QueryResult result = userDirectory.getMembersByQuery("group*", false);

    assertThat(result.get()).isEmpty();
    assertThat(result.hasException()).isFalse();
    assertThat(result.getException()).isNull();
    verify(mockCrowdClient, never()).searchGroupsByGroupNames(any());
  }

  @Test
  public void testIsGroupSearchDisabled() {
    // It's only disabled if
    // dynamic group search is disabled
    assertGroupSearchDisabled(true, true);
    // Otherwise, it's not disabled
    assertGroupSearchDisabled(false, false);
  }

  @Test
  public void testGetUsersByEmails_Saml() {
    // SAML not configured
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    SamlUser samlUser1 = tempEntity.newSamlUser("username1", null, null, "username1@example.com", null);
    SamlUser samlUser2 = tempEntity.newSamlUser("username2", null, null, "username2@example.com", null);
    tempEntity.newSamlUser("username3", null, null, "username3@example.com", null);
    Set<String> userEmails = Sets.newHashSet("username1@example.com", "username2@example.com", "username4@example.com");

    List<Member> result = userDirectory.getUsersByEmails(userEmails);
    assertThat(result).isEmpty();

    // Configure SAML
    enableSsoWithSaml();

    result = userDirectory.getUsersByEmails(userEmails);

    assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        new Member(MemberType.USER, samlUser1.getUsername(), samlUser1.calculateDisplayName(), samlUser1.getEmail(),
            SamlRealm.ID, samlUser1.getId()),
        new Member(MemberType.USER, samlUser2.getUsername(), samlUser2.calculateDisplayName(), samlUser2.getEmail(),
            SamlRealm.ID, samlUser2.getId()));
  }

  @Test
  public void testGetUsersByRealNames_OAuth2() {
    // OAuth2 not configured
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("username1", "Mark", "Mywords", null, null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("username2", "Justin", "Time", null, null);
    tempEntity.newOAuth2User("username3", "Al", "Dente", null, null);
    Set<String> userRealNames = Sets.newHashSet("Mark Mywords", "Justin Time", "James Blond");

    List<Member> result = userDirectory.getUsersByRealNames(userRealNames);
    assertThat(result).isEmpty();

    // Configure OAuth2
    enableSsoWithOAuth2();

    result = userDirectory.getUsersByRealNames(userRealNames);

    assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        new Member(MemberType.USER, oAuth2User1.getUsername(), oAuth2User1.calculateDisplayName(),
            oAuth2User1.getEmail(), OAuth2Realm.ID, oAuth2User1.getId()),
        new Member(MemberType.USER, oAuth2User2.getUsername(), oAuth2User2.calculateDisplayName(),
            oAuth2User2.getEmail(), OAuth2Realm.ID, oAuth2User2.getId()));
  }

  @Test
  public void testGetUsersByRealNames_Saml() {
    // SAML not configured
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    SamlUser samlUser1 = tempEntity.newSamlUser("username1", "Mark", "Mywords", null, null);
    SamlUser samlUser2 = tempEntity.newSamlUser("username2", "Justin", "Time", null, null);
    tempEntity.newSamlUser("username3", "Al", "Dente", null, null);
    Set<String> userRealNames = Sets.newHashSet("Mark Mywords", "Justin Time", "James Blond");

    List<Member> result = userDirectory.getUsersByRealNames(userRealNames);
    assertThat(result).isEmpty();

    // Configure SAML
    enableSsoWithSaml();

    result = userDirectory.getUsersByRealNames(userRealNames);

    assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        new Member(MemberType.USER, samlUser1.getUsername(), samlUser1.calculateDisplayName(), samlUser1.getEmail(),
            SamlRealm.ID, samlUser1.getId()),
        new Member(MemberType.USER, samlUser2.getUsername(), samlUser2.calculateDisplayName(), samlUser2.getEmail(),
            SamlRealm.ID, samlUser2.getId()));
  }

  @Test
  public void testGetUsersByEmails_OAuth2() {
    // OAuth2 not configured
    UserDirectory userDirectory =
        new UserDirectory(userDao, ldapServerDAO, ssoUserService, ldapService, crowdClientFactory);
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("username1", null, null, "username1@example.com", null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("username2", null, null, "username2@example.com", null);
    tempEntity.newOAuth2User("username3", null, null, "username3@example.com", null);
    Set<String> userEmails = Sets.newHashSet("username1@example.com", "username2@example.com", "username4@example.com");

    List<Member> result = userDirectory.getUsersByEmails(userEmails);
    assertThat(result).isEmpty();

    // Configure OAuth2
    enableSsoWithOAuth2();

    result = userDirectory.getUsersByEmails(userEmails);

    assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        new Member(MemberType.USER, oAuth2User1.getUsername(), oAuth2User1.calculateDisplayName(),
            oAuth2User1.getEmail(), OAuth2Realm.ID, oAuth2User1.getId()),
        new Member(MemberType.USER, oAuth2User2.getUsername(), oAuth2User2.calculateDisplayName(),
            oAuth2User2.getEmail(), OAuth2Realm.ID, oAuth2User2.getId()));
  }

  private void assertGroupSearchDisabled(boolean dynamicGroupSearchDisabled, boolean expectedDisabled) {
    LdapService mockLdapService = mock(LdapService.class);
    UserDirectory userDirectory = new UserDirectory(null, null, null, mockLdapService, null);
    when(mockLdapService.isDynamicGroupSearchDisabled()).thenReturn(dynamicGroupSearchDisabled);

    assertThat(userDirectory.isGroupSearchDisabled()).isEqualTo(expectedDisabled);
  }

  private LdapService mockEnabledLdapService(final String ldapServerName) {
    final LdapService mockLdapService = mock(LdapService.class);
    tempEntity.newLdapServer(ldapServerName);
    when(mockLdapService.isLdapEnabled(any(LdapServer.class))).thenReturn(true);

    return mockLdapService;
  }

  private static class SameId
      implements ArgumentMatcher<LdapServer>
  {
    private final String ldapServerId;

    SameId(LdapServer ldapServer) {
      ldapServerId = ldapServer.getId();
    }

    @Override
    public boolean matches(LdapServer other) {
      if (other == null) {
        return false;
      }
      return ldapServerId.equals(other.getId());
    }
  }
}
