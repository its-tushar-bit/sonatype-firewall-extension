/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
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
}
