/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cpematching;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class CpeMatchingConfigurationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private CpeMatchingConfigurationService cpeMatchingConfigurationService;

  @Test
  public void testGetCpeMatchingStatus_Authorized() {
    grantReadPermission(app.getId());
    cpeMatchingConfigurationService.getCpeMatchingStatus(app.getType(), app.getPublicId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetCpeMatchingStatus_Unauthorized() {
    login();
    cpeMatchingConfigurationService.getCpeMatchingStatus(app.getType(), app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetCpeMatchingStatus_Unauthenticated() {
    cpeMatchingConfigurationService.getCpeMatchingStatus(app.getType(), app.getPublicId());
  }

  @Test
  public void testUpdateCpeMatchingConfiguration_Authorized() {
    grantWritePermission(app.getId());
    CpeMatchingConfigurationRequest request = new CpeMatchingConfigurationRequest();
    request.allowOverride = true;
    request.enabled = true;
    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(app.getType(), app.getId(), request);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateCpeMatchingConfiguration_Unauthorized() {
    login();
    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(app.getType(), app.getId(), null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateCpeMatchingConfiguration_Unauthenticated() {
    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(app.getType(), app.getId(), null);
  }

  @Test
  public void testDisableCpeMatchingConfiguration_Authorized() {
    grantWritePermission(app.getId());
    cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.APPLICATION, app.getId(), null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDisableCpeMatchingConfiguration_Unauthorized() {
    login();
    cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.APPLICATION, app.getId(), null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDisableCpeMatchingConfiguration_Unauthenticated() {
    cpeMatchingConfigurationService.disableCpeMatchingConfiguration(OwnerType.APPLICATION, app.getId(), null);
  }
}
