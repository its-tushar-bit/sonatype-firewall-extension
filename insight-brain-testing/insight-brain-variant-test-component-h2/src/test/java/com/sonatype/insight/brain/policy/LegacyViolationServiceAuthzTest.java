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
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class LegacyViolationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
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

  @Test
  public void testRevokeLegacyViolationStatus_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> legacyViolationService.revokeLegacyViolationStatus(app.getPublicId()));
  }

  @Test
  public void testRevokeLegacyViolationStatus_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> legacyViolationService.revokeLegacyViolationStatus(app.getPublicId()));
  }

  @Test
  public void testGrantLegacyViolationStatus_Authorized() {
    grantWritePermission(app.getId());
    app.setLegacyViolationEnabled(true);
    applicationDAO.update(app);
    legacyViolationService.grantLegacyViolationStatus(app.getPublicId());
  }

  @Test
  public void testGrantLegacyViolationStatus_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> legacyViolationService.grantLegacyViolationStatus(app.getPublicId()));
  }

  @Test
  public void testGrantLegacyViolationStatus_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> legacyViolationService.grantLegacyViolationStatus(app.getPublicId()));
  }

  @Test
  public void testGetLegacyViolationsStatus_Application_Authorized() {
    grantReadPermission(app.getId());
    legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testGetLegacyViolationsStatus_Application_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId()));
  }

  @Test
  public void testGetLegacyViolationsStatus_Application_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> legacyViolationService.getLegacyViolationsStatus(OwnerType.APPLICATION, app.getPublicId()));
  }

  @Test
  public void testGetLegacyViolationsStatus_Organization_Authorized() {
    grantReadPermission(org.getId());
    legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testGetLegacyViolationsStatus_Organization_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetLegacyViolationsStatus_Organization_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> legacyViolationService.getLegacyViolationsStatus(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  public void testSetLegacyViolationStatus_Application_Authorized() {
    grantWritePermission(app.getId());
    legacyViolationService.setLegacyViolationStatus(OwnerType.APPLICATION, app.getPublicId(),
        new LegacyViolationStatusDTO());
  }

  @Test
  public void testSetLegacyViolationStatus_Application_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(
            () -> legacyViolationService.setLegacyViolationStatus(OwnerType.APPLICATION, app.getPublicId(), null));
  }

  @Test
  public void testSetLegacyViolationStatus_Application_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(
            () -> legacyViolationService.setLegacyViolationStatus(OwnerType.APPLICATION, app.getPublicId(), null));
  }

  @Test
  public void testSetLegacyViolationStatus_Organization_Authorized() {
    grantWritePermission(org.getId());
    legacyViolationService.setLegacyViolationStatus(OwnerType.ORGANIZATION, org.getId(),
        new LegacyViolationStatusDTO());
  }

  @Test
  public void testSetLegacyViolationStatus_Organization_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> legacyViolationService.setLegacyViolationStatus(OwnerType.ORGANIZATION, org.getId(), null));
  }

  @Test
  public void testSetLegacyViolationStatus_Organization_Unauthenticated() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> legacyViolationService.setLegacyViolationStatus(OwnerType.ORGANIZATION, org.getId(), null));
  }
}
