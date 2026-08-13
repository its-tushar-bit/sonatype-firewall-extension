/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class SuccessMetricsServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private SuccessMetricsService successMetricsService;

  @Test
  public void testGet_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> successMetricsService.get());
  }

  @Test
  public void testGet_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> successMetricsService.get());
  }

  @Test
  public void testUpdate_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> successMetricsService.update(new SuccessMetricsConfigurationDTO()));
  }

  @Test
  public void testUpdate_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> successMetricsService.update(new SuccessMetricsConfigurationDTO()));
  }

  @Test
  public void testUpdate_Authorized() {
    grantConfigureSystemPermission();
    successMetricsService.update(new SuccessMetricsConfigurationDTO());
  }
}
