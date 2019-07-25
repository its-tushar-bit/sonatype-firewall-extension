/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static com.sonatype.insight.brain.model.security.Role.DEVELOPER_ROLE_ID;
import static com.sonatype.insight.brain.model.security.Role.POLICY_ADMIN_ROLE_ID;

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
    grantWritePermission(app.getId());
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
    grantWritePermission(org.getId());
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
  public void testGrantMembershipMapping_Application_Authorized() {
    grantWritePermission(app.getId());
    membershipMappingService
        .grantMembershipMapping(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER, "username");
  }

  @Test
  public void testRevokeMembershipMapping_Application_Authorized() {
    grantWritePermission(app.getId());
    tempEntity.newMembershipMapping(app.getId(), DEVELOPER_ROLE_ID, getUsername(), MemberType.USER);
    membershipMappingService
        .revokeMembershipMapping(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER, getUsername());
  }

  @Test
  public void testGrantMembershipMapping_Organization_Authorized() {
    grantWritePermission(org.getId());
    membershipMappingService
        .grantMembershipMapping(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER, "username");
  }

  @Test
  public void testRevokeMembershipMapping_Organization_Authorized() {
    grantWritePermission(org.getId());
    tempEntity.newMembershipMapping(org.getId(), DEVELOPER_ROLE_ID, getUsername(), MemberType.USER);
    membershipMappingService
        .revokeMembershipMapping(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER,
            getUsername());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGrantMembershipMapping_Application_Unauthorized() {
    login();
    membershipMappingService
        .grantMembershipMapping(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER, "username");
  }

  @Test(expected = UnauthorizedException.class)
  public void testRevokeMembershipMapping_Application_Unauthorized() {
    login();
    membershipMappingService
        .revokeMembershipMapping(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER, getUsername());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGrantMembershipMapping_Organization_Unauthorized() {
    login();
    membershipMappingService
        .grantMembershipMapping(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER, "username");
  }

  @Test(expected = UnauthorizedException.class)
  public void testRevokeMembershipMapping_Organization_Unauthorized() {
    login();
    membershipMappingService
        .revokeMembershipMapping(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER,
            getUsername());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGrantMembershipMapping_Application_Unauthenticated() {
    membershipMappingService
        .grantMembershipMapping(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER, "username");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRevokeMembershipMapping_Application_Unauthenticated() {
    membershipMappingService
        .revokeMembershipMapping(OwnerType.APPLICATION, app.getId(), DEVELOPER_ROLE_ID, MemberType.USER, getUsername());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGrantMembershipMapping_Organization_Unauthenticated() {
    membershipMappingService
        .grantMembershipMapping(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER, "username");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRevokeMembershipMapping_Organization_Unauthenticated() {
    membershipMappingService
        .revokeMembershipMapping(OwnerType.ORGANIZATION, org.getId(), DEVELOPER_ROLE_ID, MemberType.USER,
            getUsername());
  }

  @Test
  public void testGrantMembershipMapping_Global_Authorized() {
    grantConfigureSystemPermission();
    String username = tempEntity.newUser("different-user").getUsername();
    membershipMappingService
        .grantMembershipMapping(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
            MemberType.USER, username);
  }

  @Test
  public void testRevokeMembershipMapping_Global_Authorized() {
    grantConfigureSystemPermission();
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID, getUsername(),
        MemberType.USER);
    membershipMappingService
        .revokeMembershipMapping(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
            MemberType.USER, getUsername());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGrantMembershipMapping_Global_Unauthorized() {
    login();
    String username = tempEntity.newUser("different-user").getUsername();
    membershipMappingService
        .grantMembershipMapping(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
            MemberType.USER, username);
  }

  @Test(expected = UnauthorizedException.class)
  public void testRevokeMembershipMapping_Global_Unauthorized() {
    login();
    membershipMappingService
        .revokeMembershipMapping(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
            MemberType.USER, getUsername());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGrantMembershipMapping_Global_Unauthenticated() {
    String username = tempEntity.newUser("different-user").getUsername();
    membershipMappingService
        .grantMembershipMapping(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
            MemberType.USER, username);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRevokeMembershipMapping_Global_Unauthenticated() {
    membershipMappingService
        .revokeMembershipMapping(OwnerType.GLOBAL, MembershipMapping.GLOBAL_CONTEXT_ID, POLICY_ADMIN_ROLE_ID,
            MemberType.USER, getUsername());
  }
}
