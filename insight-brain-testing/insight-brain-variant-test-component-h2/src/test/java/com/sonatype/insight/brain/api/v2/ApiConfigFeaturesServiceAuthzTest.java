/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiConfigFeaturesServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiConfigFeaturesService apiConfigFeaturesService;

  @Test
  public void testDisableFeature() {
    grantConfigureSystemPermission();
    apiConfigFeaturesService.disableFeature(ApiConfigFeaturesService.FEATURE_DASHBOARD);
  }

  @Test
  public void testDisableFeature_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiConfigFeaturesService.disableFeature(ApiConfigFeaturesService.FEATURE_DASHBOARD));
  }

  @Test
  public void testDisableFeature_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiConfigFeaturesService.disableFeature(ApiConfigFeaturesService.FEATURE_DASHBOARD));
  }

  @Test
  public void testEnableFeature() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");
    grantConfigureSystemPermission();
    apiConfigFeaturesService.enableFeature(ApiConfigFeaturesService.FEATURE_DASHBOARD);
  }

  @Test
  public void testEnableFeature_Unauthenticated() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");
    assertThrows(UnauthenticatedException.class,
        () -> apiConfigFeaturesService.enableFeature(ApiConfigFeaturesService.FEATURE_DASHBOARD));
  }

  @Test
  public void testEnableFeature_Unauthorized() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiConfigFeaturesService.enableFeature(ApiConfigFeaturesService.FEATURE_DASHBOARD));
  }
}
