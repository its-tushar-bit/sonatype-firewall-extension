/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ScanServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ScanService scanService;

  @Test(expected = UnauthenticatedException.class)
  public void testScanBinary_Anon() throws Exception {
    scanService.scanBinary(app.getPublicId(), getClass().getResourceAsStream("/ScannerTest/app01.zip"));
  }

  @Test(expected = UnauthorizedException.class)
  public void testScanBinary_Unauthorized() throws Exception {
    login();
    scanService.scanBinary(app.getPublicId(), getClass().getResourceAsStream("/ScannerTest/app01.zip"));
  }

  @Test
  public void testScanBinary_Authorized() throws Exception {
    grantWritePermission(app.getId());
    scanService.scanBinary(app.getPublicId(), getClass().getResourceAsStream("/ScannerTest/app01.zip"));
  }
}
