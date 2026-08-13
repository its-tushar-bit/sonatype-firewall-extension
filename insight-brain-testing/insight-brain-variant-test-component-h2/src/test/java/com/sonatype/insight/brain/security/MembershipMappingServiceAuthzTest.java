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
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.security.Role.DEVELOPER_ROLE_ID;
import static com.sonatype.insight.brain.model.security.Role.POLICY_ADMIN_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class MembershipMappingServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private MembershipMappingService membershipMappingService;

  @Test
  public void testGetApplicableMembershipMappings_Application_Authorized() {
    grantReadPermission(app.getId());
    membershipMappingService.getApplicableMembershipMappings(OwnerType.APPLICATION, app.getId());
  }

  @Test
  public void testGetApplicableMembershipMappings_Application_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService.getApplicableMembershipMappings(OwnerType.APPLICATION, app.getId()));
  }

  @Test
  public void testGetApplicableMembershipMappings_Application_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService.getApplicableMembershipMappings(OwnerType.APPLICATION, app.getId()));
  }

  @Test
  public void testGetApplicableMembershipMappings_Organization_Authorized() {
    grantReadPermission(org.getId());
    membershipMappingService.getApplicableMembershipMappings(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testGetApplicableMembershipMappings_Organization_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService.getApplicableMembershipMappings(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetApplicableMembershipMappings_Organization_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService.getApplicableMembershipMappings(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetApplicableMembershipMappings_RepositoryContainer_Authorized() {
    grantReadPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID);
    membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testGetApplicableMembershipMappings_RepositoryContainer_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  public void testGetApplicableMembershipMappings_RepositoryContainer_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  public void testGetApplicableMembershipMappings_RepositoryManager_Authorized() {
    grantReadPermission(repositoryManager.getId());
    membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId());
  }

  @Test
  public void testGetApplicableMembershipMappings_RepositoryManager_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY_MANAGER,
            repositoryManager.getId()));
  }

  @Test
  public void testGetApplicableMembershipMappings_RepositoryManager_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY_MANAGER,
            repositoryManager.getId()));
  }

  @Test
  public void testGetApplicableMembershipMappings_Repository_Authorized() {
    grantReadPermission(repository.getId());
    membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY, repository.getId());
  }

  @Test
  public void testGetApplicableMembershipMappings_Repository_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY, repository.getId()));
  }

  @Test
  public void testGetApplicableMembershipMappings_Repository_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService.getApplicableMembershipMappings(OwnerType.REPOSITORY, repository.getId()));
  }

  @Test
  public void testGetApplicableMembershipMappings_Global_Authorized() {
    grantConfigureSystemPermission();
    membershipMappingService.getApplicableMembershipMappings(OwnerType.GLOBAL, "ownerId");
  }

  @Test
  public void testGetApplicableMembershipMappings_Global_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService.getApplicableMembershipMappings(OwnerType.GLOBAL, "ownerId"));
  }

  @Test
  public void testGetApplicableMembershipMappings_Global_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService.getApplicableMembershipMappings(OwnerType.GLOBAL, "ownerId"));
  }

  @Test
  public void testSetMembershipMappings_Application_Authorized() {
    grantPermission(app.getId(), Permission.EDIT_ACCESS_CONTROL);
    membershipMappingService.setMembershipMappings(OwnerType.APPLICATION, app.getId(), Collections.emptyMap());
  }

  @Test
  public void testSetMembershipMappings_Application_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService.setMembershipMappings(OwnerType.APPLICATION, app.getId(),
            Collections.emptyMap()));
  }

  @Test
  public void testSetMembershipMappings_Application_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService.setMembershipMappings(OwnerType.APPLICATION, app.getId(),
            Collections.emptyMap()));
  }

  @Test
  public void testSetMembershipMappings_Organization_Authorized() {
    grantPermission(org.getId(), Permission.EDIT_ACCESS_CONTROL);
    membershipMappingService.setMembershipMappings(OwnerType.ORGANIZATION, org.getId(), Collections.emptyMap());
  }

  @Test
  public void testSetMembershipMappings_Organization_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService.setMembershipMappings(OwnerType.ORGANIZATION, org.getId(),
            Collections.emptyMap()));
  }

  @Test
  public void testSetMembershipMappings_Organization_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService.setMembershipMappings(OwnerType.ORGANIZATION, org.getId(),
            Collections.emptyMap()));
  }

  @Test
  public void testSetMembershipMappings_RepositoryContainer_Authorized() {
    grantPermission(RepositoryContainer.REPOSITORY_CONTAINER_ID, Permission.EDIT_ACCESS_CONTROL);
    membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY_CONTAINER,
        RepositoryContainer.REPOSITORY_CONTAINER_ID, Collections.emptyMap());
  }

  @Test
  public void testSetMembershipMappings_RepositoryContainer_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID, Collections.emptyMap()));
  }

  @Test
  public void testSetMembershipMappings_RepositoryContainer_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY_CONTAINER,
            RepositoryContainer.REPOSITORY_CONTAINER_ID, Collections.emptyMap()));
  }

  @Test
  public void testSetMembershipMappings_RepositoryManager_Authorized() {
    grantPermission(repositoryManager.getId(), Permission.EDIT_ACCESS_CONTROL);
    membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId(),
        Collections.emptyMap());
  }

  @Test
  public void testSetMembershipMappings_RepositoryManager_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY_MANAGER,
            repositoryManager.getId(), Collections.emptyMap()));
  }

  @Test
  public void testSetMembershipMappings_RepositoryManager_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY_MANAGER,
            repositoryManager.getId(), Collections.emptyMap()));
  }

  @Test
  public void testSetMembershipMappings_Repository_Authorized() {
    grantPermission(repository.getId(), Permission.EDIT_ACCESS_CONTROL);
    membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY, repository.getId(), Collections.emptyMap());
  }

  @Test
  public void testSetMembershipMappings_Repository_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY, repository.getId(),
            Collections.emptyMap()));
  }

  @Test
  public void testSetMembershipMappings_Repository_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService.setMembershipMappings(OwnerType.REPOSITORY, repository.getId(),
            Collections.emptyMap()));
  }

  @Test
  public void testSetMembershipMappings_Global_Authorized() {
    grantConfigureSystemPermission();
    membershipMappingService.setMembershipMappings(OwnerType.GLOBAL, "ownerId", Collections.emptyMap());
  }

  @Test
  public void testSetMembershipMappings_Global_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService.setMembershipMappings(OwnerType.GLOBAL, "ownerId", Collections.emptyMap()));
  }

  @Test
  public void testSetMembershipMappings_Global_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService.setMembershipMappings(OwnerType.GLOBAL, "ownerId", Collections.emptyMap()));
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

  @Test
  public void testGrantRoleMembership_Application_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService
            .grantRoleMembership(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER, "username"));
  }

  @Test
  public void testRevokeRoleMembership_Application_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService
            .revokeRoleMembership(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER,
                getUsername()));
  }

  @Test
  public void testGrantRoleMembership_Organization_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService
            .grantRoleMembership(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER,
                "username"));
  }

  @Test
  public void testRevokeRoleMembership_Organization_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService
            .revokeRoleMembership(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER,
                getUsername()));
  }

  @Test
  public void testGrantRoleMembership_Application_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService
            .grantRoleMembership(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER, "username"));
  }

  @Test
  public void testRevokeRoleMembership_Application_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService
            .revokeRoleMembership(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER,
                getUsername()));
  }

  @Test
  public void testGrantRoleMembership_Organization_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService
            .grantRoleMembership(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER,
                "username"));
  }

  @Test
  public void testRevokeRoleMembership_Organization_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService
            .revokeRoleMembership(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER,
                getUsername()));
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

  @Test
  public void testGrantRoleMembership_Global_Unauthorized() {
    login();
    String username = tempEntity.newUser("different-user").getUsername();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService
            .grantRoleMembership(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
                MemberType.USER, username));
  }

  @Test
  public void testRevokeRoleMembership_Global_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
        () -> membershipMappingService
            .revokeRoleMembership(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
                MemberType.USER, getUsername()));
  }

  @Test
  public void testGrantRoleMembership_Global_Unauthenticated() {
    String username = tempEntity.newUser("different-user").getUsername();
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService
            .grantRoleMembership(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
                MemberType.USER, username));
  }

  @Test
  public void testRevokeRoleMembership_Global_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(
        () -> membershipMappingService
            .revokeRoleMembership(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
                MemberType.USER, getUsername()));
  }
}
