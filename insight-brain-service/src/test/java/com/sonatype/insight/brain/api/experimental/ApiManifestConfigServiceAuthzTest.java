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

public class ApiManifestConfigServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  public ApiManifestConfigService apiManifestConfigService;

  @Test
  public void testPerformManifestScan_Authorized() {
    grantGlobalPermission(Permission.READ);

    boolean actual = apiManifestConfigService.isManifestScanFeatureEnabled();

    assertThat(actual).isFalse();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testPerformManifestScan_Unauthenticated() {
    apiManifestConfigService.isManifestScanFeatureEnabled();
  }

  @Test(expected = UnauthorizedException.class)
  public void testPerformManifestScan_Unauthorized() {
    login();
    apiManifestConfigService.isManifestScanFeatureEnabled();
  }
}
