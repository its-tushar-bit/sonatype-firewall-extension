/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.*;

public class ApiConfigFeaturesServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiConfigFeaturesService apiConfigFeaturesService;

  @Test
  public void testDisableFeature() {
    grantConfigureSystemPermission();
    apiConfigFeaturesService.disableFeature(ApiConfigFeaturesService.FEATURE_DASHBOARD);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDisableFeature_Unauthenticated() {
    apiConfigFeaturesService.disableFeature(ApiConfigFeaturesService.FEATURE_DASHBOARD);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDisableFeature_Unauthorized() {
    login();
    apiConfigFeaturesService.disableFeature(ApiConfigFeaturesService.FEATURE_DASHBOARD);
  }

  @Test
  public void testEnableFeature() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");
    grantConfigureSystemPermission();
    apiConfigFeaturesService.enableFeature(ApiConfigFeaturesService.FEATURE_DASHBOARD);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEnableFeature_Unauthenticated() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");
    apiConfigFeaturesService.enableFeature(ApiConfigFeaturesService.FEATURE_DASHBOARD);
  }

  @Test(expected = UnauthorizedException.class)
  public void testEnableFeature_Unauthorized() {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");
    login();
    apiConfigFeaturesService.enableFeature(ApiConfigFeaturesService.FEATURE_DASHBOARD);
  }
}
