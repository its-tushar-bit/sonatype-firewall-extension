/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cpematching;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class CpeMatchingConfigurationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private CpeMatchingConfigurationService cpeMatchingConfigurationService;

  @Test
  public void testGetCpeMatchingConfiguration_Authorized() {
    grantReadPermission(app.getId());
    cpeMatchingConfigurationService.getCpeMatchingConfiguration(app.getType(), app.getId());
  }

  @Test
  public void testGetCpeMatchingConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> cpeMatchingConfigurationService.getCpeMatchingConfiguration(app.getType(), app.getId()));
  }

  @Test
  public void testGetCpeMatchingConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> cpeMatchingConfigurationService.getCpeMatchingConfiguration(app.getType(), app.getId()));
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_Authorized() {
    grantWritePermission(app.getId());
    CpeMatchingConfigurationRequest request = new CpeMatchingConfigurationRequest();
    request.allowOverride = true;
    request.enabled = true;
    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(app.getType(), app.getId(), request);
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> cpeMatchingConfigurationService.updateCpeMatchingConfiguration(app.getType(), app.getId(), null));
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> cpeMatchingConfigurationService.updateCpeMatchingConfiguration(app.getType(), app.getId(), null));
  }

  @Test
  public void testDisableCpeMatchingConfiguration_Authorized() {
    grantWritePermission(app.getId());
    cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.APPLICATION, app.getId(), null);
  }

  @Test
  public void testDisableCpeMatchingConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.APPLICATION, app.getId(),
            null));
  }

  @Test
  public void testDisableCpeMatchingConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.APPLICATION, app.getId(),
            null));
  }
}
