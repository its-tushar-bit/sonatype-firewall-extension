/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserService.FindMembersDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class UserServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private UserService userService;

  @Mock
  private SessionDAO sessionDAOMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(SessionDAO.class).toInstance(sessionDAOMock);
    super.configure(binder);
  }

  @Test
  public void testGetAll_Authorized() throws Exception {
    grantConfigureSystemPermission();
    assertThat(userService.getAll(), is(not(empty())));
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAll_Unauthorized() {
    login();
    userService.getAll();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAll_Unauthenticated() {
    userService.getAll();
  }

  @Test
  public void testFindMembersForRoles_Global_Authorized() {
    grantConfigureSystemPermission();
    FindMembersDTO findMembersDTO = userService
        .findMembersForRoles(OwnerType.GLOBAL, null, "*", false /* groupsEnabled */);
    assertThat(findMembersDTO.getError(), is(nullValue()));
    assertThat(findMembersDTO.getMembers(), is(not(empty())));
  }

  @Test(expected = UnauthorizedException.class)
  public void testFindMembersForRoles_Global_Unauthorized() {
    login();
    userService.findMembersForRoles(OwnerType.GLOBAL, null, "*", false /* groupsEnabled */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testFindMembersForRoles_Global_Unauthenticated() {
    userService.findMembersForRoles(OwnerType.GLOBAL, null, "*", false /* groupsEnabled */);
  }

  @Test
  public void testFindMembersForRoles_RepositoryContainer_Authorized() {
    grantWritePermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    FindMembersDTO findMembersDTO = userService
        .findMembersForRoles(OwnerType.REPOSITORY_CONTAINER, null, "*", false /* groupsEnabled */);
    assertThat(findMembersDTO.getError(), is(nullValue()));
    assertThat(findMembersDTO.getMembers(), is(not(empty())));
  }

  @Test(expected = UnauthorizedException.class)
  public void testFindMembersForRoles_RepositoryContainer_Unauthorized() {
    login();
    userService.findMembersForRoles(OwnerType.REPOSITORY_CONTAINER, null, "*", false /* groupsEnabled */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testFindMembersForRoles_RepositoryContainer_Unauthenticated() {
    userService.findMembersForRoles(OwnerType.REPOSITORY_CONTAINER, null, "*", false /* groupsEnabled */);
  }

  @Test
  public void testFindMembersForRoles_Application_Authorized() {
    grantWritePermission(app.getId());
    FindMembersDTO findMembersDTO = userService.findMembersForRoles(OwnerType.APPLICATION, app.getPublicId(), "*",
        false /* groupsEnabled */);
    assertThat(findMembersDTO.getError(), is(nullValue()));
    assertThat(findMembersDTO.getMembers(), is(not(empty())));
  }

  @Test(expected = UnauthorizedException.class)
  public void testFindMembersForRoles_Application_Unauthorized() {
    login();
    userService.findMembersForRoles(OwnerType.APPLICATION, app.getPublicId(), "*", false /* groupsEnabled */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testFindMembersForRoles_Application_Unauthenticated() {
    userService.findMembersForRoles(OwnerType.APPLICATION, app.getPublicId(), "*", false /* groupsEnabled */);
  }

  @Test
  public void testFindMembersForRoles_Organization_Authorized() {
    grantWritePermission(org.getId());
    FindMembersDTO findMembersDTO = userService
        .findMembersForRoles(OwnerType.ORGANIZATION, org.getId(), "*", false /* groupsEnabled */);
    assertThat(findMembersDTO.getError(), is(nullValue()));
    assertThat(findMembersDTO.getMembers(), is(not(empty())));
  }

  @Test(expected = UnauthorizedException.class)
  public void testFindMembersForRoles_Organization_Unauthorized() {
    login();
    userService.findMembersForRoles(OwnerType.ORGANIZATION, org.getId(), "*", false /* groupsEnabled */);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testFindMembersForRoles_Organization_Unauthenticated() {
    userService.findMembersForRoles(OwnerType.ORGANIZATION, org.getId(), "*", false /* groupsEnabled */);
  }

  @Test
  public void testAddUser_Authorized() throws Exception {
    grantConfigureSystemPermission();
    User user = new User("testAddUser", "testAddUser", "testAddUser", "testAddUser", "testAddUser@sonatype.com");
    user = userService.addUser(user);
    new UserDAO().delete(user);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddUser_Unauthorized() throws Exception {
    login();
    User user = new User("testAddUser", "testAddUser", "testAddUser", "testAddUser", "testAddUser@sonatype.com");
    userService.addUser(user);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddUser_Unauthenticated() throws Exception {
    User user = new User("testAddUser", "testAddUser", "testAddUser", "testAddUser", "testAddUser@sonatype.com");
    userService.addUser(user);
  }

  @Test
  public void testUpdateUser_Authorized() throws Exception {
    grantConfigureSystemPermission();
    User user = tempEntity.newUser("testUpdateUser");
    user.setPassword(UserService.FAKE_PASSWORD);
    userService.updateUser(user);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateUser_Unauthorized() throws Exception {
    login();
    User user = tempEntity.newUser("testUpdateUser");
    userService.updateUser(user);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateUser_Unauthenticated() throws Exception {
    User user = tempEntity.newUser("testUpdateUser");
    userService.updateUser(user);
  }

  @Test
  public void testDeleteUser_Authorized() throws Exception {
    grantConfigureSystemPermission();
    User user = tempEntity.newUser("testDeleteUser");
    userService.deleteUser(user.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteUser_Unauthorized() throws Exception {
    login();
    User user = tempEntity.newUser("testDeleteUser");
    userService.deleteUser(user.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteUser_Unauthenticated() throws Exception {
    User user = tempEntity.newUser("testDeleteUser");
    userService.deleteUser(user.getId());
  }

  @Test
  public void testResetPassword_Authorized() throws Exception {
    grantConfigureSystemPermission();
    User user = tempEntity.newUser("testUpdateUser");
    userService.resetPassword(user.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testResetPassword_Unauthorized() throws Exception {
    login();
    User user = tempEntity.newUser("testUpdateUser");
    userService.resetPassword(user.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testResetPassword_Unauthenticated() throws Exception {
    User user = tempEntity.newUser("testUpdateUser");
    userService.resetPassword(user.getId());
  }

  @Test
  public void testShouldDisplayDefaultPasswordWarning_Authorized() {
    grantConfigureSystemPermission();
    userService.shouldDisplayDefaultPasswordWarning();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testShouldDisplayDefaultPasswordWarning_Unauthenticated() {
    userService.shouldDisplayDefaultPasswordWarning();
  }

  @Test(expected = UnauthorizedException.class)
  public void testShouldDisplayDefaultPasswordWarning_Unauthorized() {
    login();
    userService.shouldDisplayDefaultPasswordWarning();
  }
}
