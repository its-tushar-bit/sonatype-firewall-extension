/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.InputStream;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ScanServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ScanService scanService;

  private InputStream getBundle(String name) {
    return getClass().getResourceAsStream("/ScannerTest/" + name);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testScanBinary_Anon() throws Exception {
    scanService.scanBinary(app.getPublicId(), getBundle("app01.zip"), "app.zip", new Stage(Stage.ID_BUILD));
  }

  @Test(expected = UnauthorizedException.class)
  public void testScanBinary_Unauthorized() throws Exception {
    login();
    scanService.scanBinary(app.getPublicId(), getBundle("app01.zip"), "app.zip", new Stage(Stage.ID_BUILD));
  }

  @Test
  public void testScanBinary_Authorized() throws Exception {
    grantWritePermission(app.getId());
    scanService.scanBinary(app.getPublicId(), getBundle("app01.zip"), "app.zip", new Stage(Stage.ID_BUILD));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetTicket_Anon() throws Exception {
    scanService.getTicket("any-app-id", "any-ticket-id");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetTicket_Unauthorized() throws Exception {
    login();
    scanService.getTicket(app.getPublicId(), "any-ticket-id");
  }

  @Test
  public void testGetTicket_Authorized() throws Exception {
    grantWritePermission(app.getId());
    try {
      scanService.getTicket(app.getPublicId(), "any-ticket-id");
    }
    catch (NotFoundException irrelevant) {
      // Expected but irrelevant for this test.
    }
  }
}
