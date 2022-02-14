/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiUserDTO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.UserService.FindMembersDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.inject.Binder;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.junit.Test;
import org.mockito.Mock;

import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.createUserDTOToAdd;
import static com.sonatype.insight.brain.api.v2.ApiUserTestSupport.createUserDTOToUpdate;
import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(userService.getAll()).isNotEmpty();
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
    assertThat(findMembersDTO.getError()).isNull();
    assertThat(findMembersDTO.getMembers()).isNotEmpty();
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
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.EDIT_ACCESS_CONTROL);
    FindMembersDTO findMembersDTO = userService
        .findMembersForRoles(OwnerType.REPOSITORY_CONTAINER, null, "*", false /* groupsEnabled */);
    assertThat(findMembersDTO.getError()).isNull();
    assertThat(findMembersDTO.getMembers()).isNotEmpty();
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
    grantPermission(app.getId(), Permission.EDIT_ACCESS_CONTROL);
    FindMembersDTO findMembersDTO = userService.findMembersForRoles(OwnerType.APPLICATION, app.getPublicId(), "*",
        false /* groupsEnabled */);
    assertThat(findMembersDTO.getError()).isNull();
    assertThat(findMembersDTO.getMembers()).isNotEmpty();
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
    grantPermission(org.getId(), Permission.EDIT_ACCESS_CONTROL);
    FindMembersDTO findMembersDTO = userService
        .findMembersForRoles(OwnerType.ORGANIZATION, org.getId(), "*", false /* groupsEnabled */);
    assertThat(findMembersDTO.getError()).isNull();
    assertThat(findMembersDTO.getMembers()).isNotEmpty();
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

  @Test
  public void testGetAllApiUserDTOs_Authorized() {
    grantConfigureSystemPermission();
    assertThat(userService.getAllApiUserDTOs(User.INTERNAL_REALM_ID)).isNotNull();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAllApiUserDTOs_Unauthorized() {
    login();
    userService.getAllApiUserDTOs(User.INTERNAL_REALM_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAllApiUserDTOs_Unauthenticated() {
    userService.getAllApiUserDTOs(User.INTERNAL_REALM_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApiUserDTOByUsername_Unauthenticated() {
    userService.getApiUserDTOByUsername(tempEntity.newUser().getUsername());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApiUserDTOByUsername_Unauthorized() {
    login();
    userService.getApiUserDTOByUsername(tempEntity.newUser().getUsername());
  }

  @Test
  public void testGetApiUserDTOByUsername_Authorized() {
    grantConfigureSystemPermission();
    userService.getApiUserDTOByUsername(tempEntity.newUser().getUsername());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddUser_ByApiUserDTO_Unauthenticated() {
    ApiUserDTO inputUserDTO = createUserDTOToAdd();
    tempEntity.registerUsernames(inputUserDTO.username);
    userService.addUser(inputUserDTO);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddUser_ByApiUserDTO_Unauthorized() {
    login();
    ApiUserDTO inputUserDTO = createUserDTOToAdd();
    tempEntity.registerUsernames(inputUserDTO.username);
    userService.addUser(inputUserDTO);
  }

  @Test
  public void testAddUser_ByApiUserDTO_Authorized() {
    grantConfigureSystemPermission();
    ApiUserDTO inputUserDTO = createUserDTOToAdd();
    tempEntity.registerUsernames(inputUserDTO.username);
    userService.addUser(inputUserDTO);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateUser_ByApiUserDTO_Unauthenticated() {
    User user = tempEntity.newUser();
    userService.updateUser(user.getUsername(), createUserDTOToUpdate(user));
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateUser_ByApiUserDTO_Unauthorized() {
    login();
    User user = tempEntity.newUser();
    userService.updateUser(user.getUsername(), createUserDTOToUpdate(user));
  }

  @Test
  public void testUpdateUser_ByApiUserDTO_Authorized() {
    grantConfigureSystemPermission();
    User user = tempEntity.newUser();
    userService.updateUser(user.getUsername(), createUserDTOToUpdate(user));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteUserByUsername_Unauthenticated() {
    userService.deleteUserByUsername(tempEntity.newUser().getUsername());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteUserByUsername_Unauthorized() {
    login();
    userService.deleteUserByUsername(tempEntity.newUser().getUsername());
  }

  @Test
  public void testDeleteUserByUsername_Authorized() {
    grantConfigureSystemPermission();
    userService.deleteUserByUsername(tempEntity.newUser().getUsername());
  }
}
