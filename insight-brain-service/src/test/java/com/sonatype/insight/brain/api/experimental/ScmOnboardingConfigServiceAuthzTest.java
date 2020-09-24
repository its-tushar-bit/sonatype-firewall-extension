/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScmOnboardingConfigServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  public ScmOnboardingConfigService scmOnboardingConfigService;

  @Test
  public void testPerformManifestScan_Authorized() {
    grantGlobalPermission(Permission.READ);

    boolean actual = scmOnboardingConfigService.isScmOnboardingEnabled();

    assertThat(actual).isFalse();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testPerformManifestScan_Unauthenticated() {
    scmOnboardingConfigService.isScmOnboardingEnabled();
  }

  @Test(expected = UnauthorizedException.class)
  public void testPerformManifestScan_Unauthorized() {
    login();
    scmOnboardingConfigService.isScmOnboardingEnabled();
  }
}
