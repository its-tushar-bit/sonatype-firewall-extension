/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiSastScanServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiSastScanService apiSastScanService;

  @Test
  public void testGetSastScan_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiSastScanService.getSastScan(app.getPublicId(), ""));
  }

  @Test
  public void testGetSastScan_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiSastScanService.getSastScan(app.getPublicId(), ""));
  }

  @Test
  public void testCreateSastScan_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> apiSastScanService.createSastScan(app.getPublicId(), new SastScanRequestDTO()));
  }

  @Test
  public void testCreateSastScan_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiSastScanService.createSastScan(app.getPublicId(), new SastScanRequestDTO()));
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
