/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.policy.LegacyViolationService.LegacyViolationStatusDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class LegacyViolationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private LegacyViolationService legacyViolationService;

  @Test
  public void testRevokeLegacyViolationStatus_Authorized() {
    grantWritePermission(app.getId());
    legacyViolationService.revokeLegacyViolationStatus(app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testRevokeLegacyViolationStatus_Unauthorized() {
    login();
    legacyViolationService.revokeLegacyViolationStatus(app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testRevokeLegacyViolationStatus_Unauthenticated() {
    legacyViolationService.revokeLegacyViolationStatus(app.getPublicId());
  }

  @Test
  public void testGrantLegacyViolationStatus_Authorized() {
    grantWritePermission(app.getId());
    app.setLegacyViolationEnabled(true);
    applicationDAO.update(app);
    legacyViolationService.grantLegacyViolationStatus(app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGrantLegacyViolationStatus_Unauthorized() {
    login();
    legacyViolationService.grantLegacyViolationStatus(app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGrantLegacyViolationStatus_Unauthenticated() {
    legacyViolationService.grantLegacyViolationStatus(app.getPublicId());
  }

  @Test
  public void testGetLegacyViolationsStatus_Application_Authorized() {
    grantReadPermission(app.getId());
    legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLegacyViolationsStatus_Application_Unauthorized() {
    login();
    legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLegacyViolationsStatus_Application_Unauthenticated() {
    legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testGetLegacyViolationsStatus_Organization_Authorized() {
    grantReadPermission(org.getId());
    legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetLegacyViolationsStatus_Organization_Unauthorized() {
    login();
    legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetLegacyViolationsStatus_Organization_Unauthenticated() {
    legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testSetLegacyViolationStatus_Application_Authorized() {
    grantWritePermission(app.getId());
    legacyViolationService.setLegacyViolationStatus(OwnerType.APPLICATION, app.getPublicId(),
        new LegacyViolationStatusDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetLegacyViolationStatus_Application_Unauthorized() {
    login();
    legacyViolationService.setLegacyViolationStatus(OwnerType.APPLICATION, app.getPublicId(), null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetLegacyViolationStatus_Application_Unauthenticated() {
    legacyViolationService.setLegacyViolationStatus(OwnerType.APPLICATION, app.getPublicId(), null);
  }

  @Test
  public void testSetLegacyViolationStatus_Organization_Authorized() {
    grantWritePermission(org.getId());
    legacyViolationService.setLegacyViolationStatus(OwnerType.ORGANIZATION, org.getId(),
        new LegacyViolationStatusDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetLegacyViolationStatus_Organization_Unauthorized() {
    login();
    legacyViolationService.setLegacyViolationStatus(OwnerType.ORGANIZATION, org.getId(), null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetLegacyViolationStatus_Organization_Unauthenticated() {
    legacyViolationService.setLegacyViolationStatus(OwnerType.ORGANIZATION, org.getId(), null);
  }
}
