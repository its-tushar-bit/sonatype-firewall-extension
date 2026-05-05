/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static com.sonatype.insight.brain.model.security.Role.DEVELOPER_ROLE_ID;
import static com.sonatype.insight.brain.model.security.Role.POLICY_ADMIN_ROLE_ID;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class MembershipMappingServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private MembershipMappingService membershipMappingService;

  @Test
  public void testGetApplicableMembershipMappings_Application_Authorized() {
    grantReadPermission(app.getId());
    membershipMappingService.getApplicableMembershipMappings(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableMembershipMappings_Application_Unauthenticated() {
    membershipMappingService.getApplicableMembershipMappings(OwnerType.APPLICATION, app.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableMembershipMappings_Application_Unauthorized() {
    login();
    membershipMappingService.getApplicableMembershipMappings(OwnerType.APPLICATION, app.getId());
  }

  @Test
  public void testGetApplicableMembershipMappings_Organization_Authorized() {
    grantReadPermission(org.getId());
    membershipMappingService.getApplicableMembershipMappings(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableMembershipMappings_Organization_Unauthenticated() {
    membershipMappingService.getApplicableMembershipMappings(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableMembershipMappings_Organization_Unauthorized() {
    login();
    membershipMappingService.getApplicableMembershipMappings(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testGetApplicableMembershipMappings_RepositoryContainer_Authorized() {
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableMembershipMappings_RepositoryContainer_Unauthenticated() {
    membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableMembershipMappings_RepositoryContainer_Unauthorized() {
    login();
    membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testGetApplicableMembershipMappings_RepositoryManager_Authorized() {
    grantReadPermission(repositoryManager.getId());
    membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableMembershipMappings_RepositoryManager_Unauthenticated() {
    membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableMembershipMappings_RepositoryManager_Unauthorized() {
    login();
    membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId());
  }

  @Test
  public void testGetApplicableMembershipMappings_Repository_Authorized() {
    grantReadPermission(repository.getId());
    membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY, repository.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableMembershipMappings_Repository_Unauthenticated() {
    membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY, repository.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableMembershipMappings_Repository_Unauthorized() {
    login();
    membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY, repository.getId());
  }

  @Test
  public void testGetApplicableMembershipMappings_Global_Authorized() {
    grantConfigureSystemPermission();
    membershipMappingService.getApplicableMembershipMappings(OwnerType.GLOBAL, "ownerId");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicableMembershipMappings_Global_Unauthenticated() {
    membershipMappingService.getApplicableMembershipMappings(OwnerType.GLOBAL, "ownerId");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetApplicableMembershipMappings_Global_Unauthorized() {
    login();
    membershipMappingService.getApplicableMembershipMappings(OwnerType.GLOBAL, "ownerId");
  }

  @Test
  public void testSetMembershipMappings_Application_Authorized() {
    grantPermission(app.getId(), Permission.EDIT_ACCESS_CONTROL);
    membershipMappingService.setMembershipMappings(OwnerType.APPLICATION, app.getId(), Collections.emptyMap());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetMembershipMappings_Application_Unauthenticated() {
    membershipMappingService.setMembershipMappings(OwnerType.APPLICATION, app.getId(), Collections.emptyMap());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetMembershipMappings_Application_Unauthorized() {
    login();
    membershipMappingService.setMembershipMappings(OwnerType.APPLICATION, app.getId(), Collections.emptyMap());
  }

  @Test
  public void testSetMembershipMappings_Organization_Authorized() {
    grantPermission(org.getId(), Permission.EDIT_ACCESS_CONTROL);
    membershipMappingService.setMembershipMappings(OwnerType.ORGANIZATION, org.getId(), Collections.emptyMap());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetMembershipMappings_Organization_Unauthenticated() {
    membershipMappingService.setMembershipMappings(OwnerType.ORGANIZATION, org.getId(), Collections.emptyMap());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetMembershipMappings_Organization_Unauthorized() {
    login();
    membershipMappingService.setMembershipMappings(OwnerType.ORGANIZATION, org.getId(), Collections.emptyMap());
  }

  @Test
  public void testSetMembershipMappings_RepositoryContainer_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.EDIT_ACCESS_CONTROL);
    membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, Collections.emptyMap());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetMembershipMappings_RepositoryContainer_Unauthenticated() {
    membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, Collections.emptyMap());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetMembershipMappings_RepositoryContainer_Unauthorized() {
    login();
    membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, Collections.emptyMap());
  }

  @Test
  public void testSetMembershipMappings_RepositoryManager_Authorized() {
    grantPermission(repositoryManager.getId(), Permission.EDIT_ACCESS_CONTROL);
    membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(),
        Collections.emptyMap());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetMembershipMappings_RepositoryManager_Unauthenticated() {
    membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(),
        Collections.emptyMap());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetMembershipMappings_RepositoryManager_Unauthorized() {
    login();
    membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(),
        Collections.emptyMap());
  }

  @Test
  public void testSetMembershipMappings_Repository_Authorized() {
    grantPermission(repository.getId(), Permission.EDIT_ACCESS_CONTROL);
    membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY, repository.getId(), Collections.emptyMap());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetMembershipMappings_Repository_Unauthenticated() {
    membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY, repository.getId(), Collections.emptyMap());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetMembershipMappings_Repository_Unauthorized() {
    login();
    membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY, repository.getId(), Collections.emptyMap());
  }

  @Test
  public void testSetMembershipMappings_Global_Authorized() {
    grantConfigureSystemPermission();
    membershipMappingService.setMembershipMappings(OwnerType.GLOBAL, "ownerId", Collections.emptyMap());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetMembershipMappings_Global_Unauthenticated() {
    membershipMappingService.setMembershipMappings(OwnerType.GLOBAL, "ownerId", Collections.emptyMap());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetMembershipMappings_Global_Unauthorized() {
    login();
    membershipMappingService.setMembershipMappings(OwnerType.GLOBAL, "ownerId", Collections.emptyMap());
  }

  @Test
  public void testGrantRoleMembership_Application_Authorized() {
    grantPermission(app.getId(), Permission.EDIT_ACCESS_CONTROL);
    membershipMappingService
        .grantRoleMembership(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER, "username");
  }

  @Test
  public void testRevokeRoleMembership_Application_Authorized() {
    grantPermission(app.getId(), Permission.EDIT_ACCESS_CONTROL);
    tempEntity.newMembershipMapping(app.getId(), DEVELOPER_ROLE_ID, getUsername(), MemberType.USER);
    membershipMappingService
        .revokeRoleMembership(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER, getUsername());
  }

  @Test
  public void testGrantRoleMembership_Organization_Authorized() {
    grantPermission(org.getId(), Permission.EDIT_ACCESS_CONTROL);
    membershipMappingService
        .grantRoleMembership(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER, "username");
  }

  @Test
  public void testRevokeRoleMembership_Organization_Authorized() {
    grantPermission(org.getId(), Permission.EDIT_ACCESS_CONTROL);
    tempEntity.newMembershipMapping(org.getId(), DEVELOPER_ROLE_ID, getUsername(), MemberType.USER);
    membershipMappingService
        .revokeRoleMembership(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER,
            getUsername());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGrantRoleMembership_Application_Unauthorized() {
    login();
    membershipMappingService
        .grantRoleMembership(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER, "username");
  }

  @Test(expected = UnauthorizedException.class)
  public void testRevokeRoleMembership_Application_Unauthorized() {
    login();
    membershipMappingService
        .revokeRoleMembership(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER, getUsername());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGrantRoleMembership_Organization_Unauthorized() {
    login();
    membershipMappingService
        .grantRoleMembership(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER, "username");
  }

  @Test(expected = UnauthorizedException.class)
  public void testRevokeRoleMembership_Organization_Unauthorized() {
    login();
    membershipMappingService
        .revokeRoleMembership(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER,
            getUsername());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGrantRoleMembership_Application_Unauthenticated() {
    membershipMappingService
        .grantRoleMembership(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER, "username");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRevokeRoleMembership_Application_Unauthenticated() {
    membershipMappingService
        .revokeRoleMembership(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER, getUsername());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGrantRoleMembership_Organization_Unauthenticated() {
    membershipMappingService
        .grantRoleMembership(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER, "username");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRevokeRoleMembership_Organization_Unauthenticated() {
    membershipMappingService
        .revokeRoleMembership(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER,
            getUsername());
  }

  @Test
  public void testGrantRoleMembership_Global_Authorized() {
    grantConfigureSystemPermission();
    String username = tempEntity.newUser("different-user").getUsername();
    membershipMappingService
        .grantRoleMembership(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
            MemberType.USER, username);
  }

  @Test
  public void testRevokeRoleMembership_Global_Authorized() {
    grantConfigureSystemPermission();
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID, getUsername(),
        MemberType.USER);
    membershipMappingService
        .revokeRoleMembership(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
            MemberType.USER, getUsername());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGrantRoleMembership_Global_Unauthorized() {
    login();
    String username = tempEntity.newUser("different-user").getUsername();
    membershipMappingService
        .grantRoleMembership(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
            MemberType.USER, username);
  }

  @Test(expected = UnauthorizedException.class)
  public void testRevokeRoleMembership_Global_Unauthorized() {
    login();
    membershipMappingService
        .revokeRoleMembership(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
            MemberType.USER, getUsername());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGrantRoleMembership_Global_Unauthenticated() {
    String username = tempEntity.newUser("different-user").getUsername();
    membershipMappingService
        .grantRoleMembership(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
            MemberType.USER, username);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRevokeRoleMembership_Global_Unauthenticated() {
    membershipMappingService
        .revokeRoleMembership(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
            MemberType.USER, getUsername());
  }
}
