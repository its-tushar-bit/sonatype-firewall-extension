/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.model.security.User;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class UserValidationServiceTest
{

  private static final String TEST_MESSAGE = "Test Exception Message";

  private LdapManager mockLdapManager;

  private UserDAO mockUserDAO;

  private UserValidationService validationService;

  private static final String userId = "testUserId";

  private static final String userName = "testUserName";

  @Before
  public void setUp() {
    mockLdapManager = Mockito.mock(LdapManager.class);
    mockUserDAO = Mockito.mock(UserDAO.class);
    validationService = new UserValidationService(mockUserDAO, mockLdapManager);
  }


  @Test
  public void testValidateUsers_NullSet() {
    Set<String> invalidUsers = validationService.validateUsers(null);
    assertThat(invalidUsers, hasSize(0));
  }

  @Test
  public void testValidateUsers_EmptySet() {
    Set<String> users = Collections.emptySet();
    Set<String> invalidUsers = validationService.validateUsers(users);
    assertThat(invalidUsers, hasSize(0));
  }

  @Test
  public void testValidateUsers_ClmUserFound() {
    User user = createUser(userId, userName);
    when(mockUserDAO.getByUsername(userName)).thenReturn(user);

    Set<String> users = Collections.singleton(userName);
    Set<String> invalidUsers = validationService.validateUsers(users);
    assertThat(invalidUsers, hasSize(0));
  }

  @Test
  public void testValidateUsers_LdapUserFound() throws Exception {
    when(mockUserDAO.getByUsername(userName)).thenReturn(null);
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);

    List<LdapUser> ldapUsers = new ArrayList<>();
    LdapUser ldapUser = createLdapUser(userName);
    ldapUsers.add(ldapUser);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenReturn(ldapUsers);

    Set<String> users = new HashSet<>();
    users.add(userName);
    Set<String> invalidUsers = validationService.validateUsers(users);
    assertThat(invalidUsers, hasSize(0));
  }

  @Test
  public void testValidateUsers_ClmUserNotFound_LdapNotConfigured() {
    when(mockUserDAO.getByUsername(userName)).thenReturn(null);
    when(mockLdapManager.isLdapEnabled()).thenReturn(false);

    Set<String> users = new HashSet<>();
    users.add(userName);
    Set<String> invalidUsers = validationService.validateUsers(users);
    assertThat(invalidUsers, hasSize(1));
    assertThat(invalidUsers, containsInAnyOrder(userName));
  }

  @Test
  public void testValidateUsers_ClmUserNotFound_LdapConfigured() throws Exception {
    when(mockUserDAO.getByUsername(userName)).thenReturn(null);
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    List<LdapUser> ldapUsers = Collections.emptyList();
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenReturn(ldapUsers);

    Set<String> users = new HashSet<>();
    users.add(userName);
    Set<String> invalidUsers = validationService.validateUsers(users);
    assertThat(invalidUsers, hasSize(1));
    assertThat(invalidUsers, containsInAnyOrder(userName));
  }

  @Test
  public void testValidateUsers_LdapErrorOnGetUsers() throws NamingException {
    when(mockUserDAO.getByUsername(userName)).thenReturn(null);
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenThrow(new NamingException(TEST_MESSAGE));

    Set<String> users = new HashSet<>();
    users.add(userName);
    Set<String> invalidUsers = validationService.validateUsers(users);
    assertThat(invalidUsers, hasSize(1));
    assertThat(invalidUsers, containsInAnyOrder(userName));
  }

  private LdapUser createLdapUser(String internalName) {
    LdapUser user = new LdapUser();
    user.setUsername(internalName);
    return user;
  }

  private User createUser(String id, String userName) {
    User user = new User();
    user.setId(id);
    user.setUsername(userName);
    return user;
  }
}
