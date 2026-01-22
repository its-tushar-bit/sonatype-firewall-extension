/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class SuccessMetricsServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private SuccessMetricsService successMetricsService;

  @Test(expected = UnauthenticatedException.class)
  public void testGet_Unauthenticated() {
    successMetricsService.get();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGet_Unauthorized() {
    login();
    successMetricsService.get();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdate_Unauthenticated() {
    successMetricsService.update(new SuccessMetricsConfigurationDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdate_Unauthorized() {
    login();
    successMetricsService.update(new SuccessMetricsConfigurationDTO());
  }

  @Test
  public void testUpdate_Authorized() {
    grantConfigureSystemPermission();
    successMetricsService.update(new SuccessMetricsConfigurationDTO());
  }
}
