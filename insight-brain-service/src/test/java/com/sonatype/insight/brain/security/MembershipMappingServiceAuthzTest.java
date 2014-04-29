/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class MembershipMappingServiceAuthzTest
    extends AbstractServiceAuthzTest
{

  @Inject
  private MembershipMappingService membershipMappingService;

  @Test
  public void testGetApplicationMembershipMappingsByPublicId_Authorized() {
    grantReadPermission(app.getId());
    membershipMappingService.getApplicableMembershipMappingsByPublicId("application", app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicationMembershipMappingsByPublicId_Unauthenticated() {
    membershipMappingService.getApplicableMembershipMappingsByPublicId("application", app.getPublicId());
  }

  @Test
  public void testGetOrganizationMembershipMappingsByPublicId_Authorized() {
    grantReadPermission(org.getId());
    membershipMappingService.getApplicableMembershipMappingsByPublicId("organization", org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetOrganizatioMembershipMappingsByPublicId_Unauthenticated() {
    membershipMappingService.getApplicableMembershipMappingsByPublicId("organization", org.getId());
  }

  @Test
  public void testGetApplicationMembershipMappingsByInternalId_Authorized() {
    grantReadPermission(app.getId());
    membershipMappingService.getApplicableMembershipMappingsByInternalId("application", app.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetApplicationMembershipMappingsByInternalId_Unauthenticated() {
    membershipMappingService.getApplicableMembershipMappingsByInternalId("application", app.getId());
  }

  @Test
  public void testGetOrganizationMembershipMappingsByInternalId_Authorized() {
    grantReadPermission(org.getId());
    membershipMappingService.getApplicableMembershipMappingsByInternalId("organization", org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetOrganizatioMembershipMappingsByInternalId_Unauthenticated() {
    membershipMappingService.getApplicableMembershipMappingsByInternalId("organization", org.getId());
  }

  @Test
  public void testSetMembershipMappingForRolesByInternalId_Authorized() {
    grantWritePermission(app.getId());
    membershipMappingService.setMembershipMappingForRolesByInternalId("application", app.getId(),
        Collections.<String, List<Member>>emptyMap());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetMembershipMappingForRolesByInternalId_Unauthenticated() {
    membershipMappingService.setMembershipMappingForRolesByInternalId("application", app.getId(),
        Collections.<String, List<Member>>emptyMap());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetMembershipMappingForRolesByInternalId_Unauthorized() {
    login();
    membershipMappingService.setMembershipMappingForRolesByInternalId("application", app.getId(),
        Collections.<String, List<Member>>emptyMap());
  }

  @Test
  public void testSetMembershipMappingForRoleByPublicId_Authorized() {
    grantWritePermission(app.getId());
    try {
      membershipMappingService
          .setMembershipMappingForRoleByPublicId("application", app.getPublicId(), "", Collections.<Member>emptyList());
    } catch (NotFoundException ignore) {
      // This is an expected exception as the roleId is empty string
    }
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetMembershipMappingForRoleByPublicId_Unauthenticated() {
    membershipMappingService.setMembershipMappingForRoleByPublicId("application", app.getPublicId(), "",
        Collections.<Member>emptyList());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetMembershipMappingForRoleByPublicId_Unauthorized() {
    login();
    membershipMappingService.setMembershipMappingForRoleByPublicId("application", app.getPublicId(), "",
        Collections.<Member>emptyList());
  }
}
