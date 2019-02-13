/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

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
}
