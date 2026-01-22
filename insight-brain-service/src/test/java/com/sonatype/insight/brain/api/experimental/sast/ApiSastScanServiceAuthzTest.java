/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiSastScanServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiSastScanService apiSastScanService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetSastScan_Unauthenticated() {
    apiSastScanService.getSastScan(app.getPublicId(), "");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetSastScan_Unauthorized() {
    login();
    apiSastScanService.getSastScan(app.getPublicId(), "");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testCreateSastScan_Unauthenticated() throws Exception {
    apiSastScanService.createSastScan(app.getPublicId(), new SastScanRequestDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testCreateSastScan_Unauthorized() throws Exception {
    login();
    apiSastScanService.createSastScan(app.getPublicId(), new SastScanRequestDTO());
  }

  @Test
  public void testGetSastScan_Authorized() {
    final String sastScanId = tempEntity.newSastScan(app.getId()).getId();
    grantReadPermission(app.getId());
    apiSastScanService.getSastScan(app.getPublicId(), sastScanId);
  }

  @Test
  public void testCreateSastScan_Authorized() throws Exception {
    grantEvaluateApplicationPermission(app.getId());
    apiSastScanService.createSastScan(app.getPublicId(), new SastScanRequestDTO());
  }
}
