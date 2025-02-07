/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.roi;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class RoiConfigurationAuthzTest extends AbstractServiceAuthzTest
{
  @Inject
  private RoiConfigurationService roiConfigurationService;

  @Test
  public void testGetCurrentAndMinimumValuesByCurrencyType() {
    grantConfigureSystemPermission();
    roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType("usd");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetCurrentAndMinimumValuesByCurrencyType_Unauthenticated() {
    roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType("usd");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetCurrentAndMinimumValuesByCurrencyType_UnauthorizedException() {
    login();
    roiConfigurationService.getCurrentAndMinimumValuesByCurrencyType("usd");
  }
}
