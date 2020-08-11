/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiManifestScanServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  public ApiManifestScanService apiManifestScanService;

  @Test
  public void testGetCompositeSourceControlByOwner_Authorized() throws Exception {
    grantEvaluateApplicationPermission(app.getId());

    apiManifestScanService.performManifestScan(app.getId(), "stage");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetCompositeSourceControlByOwner_Unauthenticated() {
    apiManifestScanService.performManifestScan(app.getId(), "stage");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetCompositeSourceControlByOwner_Unauthorized() {
    login();
    apiManifestScanService.performManifestScan(app.getId(), "stage");
  }
}
