/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.policy.PolicyViolationGrandfatheringService.PolicyViolationGrandfatheringDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class PolicyViolationGrandfatheringServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private PolicyViolationGrandfatheringService policyViolationGrandfatheringService;

  @Test
  public void testRevokeGrandfathering_Authorized() {
    grantWritePermission(app.getId());
    policyViolationGrandfatheringService.revokeGrandfathering(app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testRevokeGrandfathering_Unauthorized() {
    login();
    policyViolationGrandfatheringService.revokeGrandfathering(app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRevokeGrandfathering_Unauthenticated() {
    policyViolationGrandfatheringService.revokeGrandfathering(app.getPublicId());
  }

  @Test
  public void testGrandfather_Authorized() {
    grantWritePermission(app.getId());
    app.setPolicyViolationGrandfatheringEnabled(true);
    new ApplicationDAO().update(app);
    policyViolationGrandfatheringService.grandfather(app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGrandfather_Unauthorized() {
    login();
    policyViolationGrandfatheringService.grandfather(app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGrandfather_Unauthenticated() {
    policyViolationGrandfatheringService.grandfather(app.getPublicId());
  }

  @Test
  public void testGetGrandfathering_Application_Authorized() {
    grantReadPermission(app.getId());
    policyViolationGrandfatheringService.getGrandfathering(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetGrandfathering_Application_Unauthorized() {
    login();
    policyViolationGrandfatheringService.getGrandfathering(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetGrandfathering_Application_Unauthenticated() {
    policyViolationGrandfatheringService.getGrandfathering(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testGetGrandfathering_Organization_Authorized() {
    grantReadPermission(org.getId());
    policyViolationGrandfatheringService.getGrandfathering(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetGrandfathering_Organization_Unauthorized() {
    login();
    policyViolationGrandfatheringService.getGrandfathering(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetGrandfathering_Organization_Unauthenticated() {
    policyViolationGrandfatheringService.getGrandfathering(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testSetGrandfathering_Application_Authorized() {
    grantWritePermission(app.getId());
    policyViolationGrandfatheringService.setGrandfathering(OwnerType.APPLICATION, app.getPublicId(),
        new PolicyViolationGrandfatheringDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetGrandfathering_Application_Unauthorized() {
    login();
    policyViolationGrandfatheringService.setGrandfathering(OwnerType.APPLICATION, app.getPublicId(), null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetGrandfathering_Application_Unauthenticated() {
    policyViolationGrandfatheringService.setGrandfathering(OwnerType.APPLICATION, app.getPublicId(), null);
  }

  @Test
  public void testSetGrandfathering_Organization_Authorized() {
    grantWritePermission(org.getId());
    policyViolationGrandfatheringService.setGrandfathering(OwnerType.ORGANIZATION, org.getId(),
        new PolicyViolationGrandfatheringDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetGrandfathering_Organization_Unauthorized() {
    login();
    policyViolationGrandfatheringService.setGrandfathering(OwnerType.ORGANIZATION, org.getId(), null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetGrandfathering_Organization_Unauthenticated() {
    policyViolationGrandfatheringService.setGrandfathering(OwnerType.ORGANIZATION, org.getId(), null);
  }
}
