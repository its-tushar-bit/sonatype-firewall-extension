/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class ScanServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ScanService scanService;

  private InputStream getBundle() {
    return new ByteArrayInputStream(new byte[0]);
  }

  @Test
  public void testScanBinary_Anon() {
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(() -> scanService.scanBinary(
        app.getPublicId(), getBundle(), "app.zip", new Stage(Stage.ID_BUILD), false, null, null, null));
  }

  @Test
  public void testScanBinary_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(() -> scanService.scanBinary(
        app.getPublicId(), getBundle(), "app.zip", new Stage(Stage.ID_BUILD), false, null, null, null));
  }

  @Test
  @Timeout(15)
  public void testScanBinary_Authorized() throws Exception {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    ScanTicket originalTicket = scanService.scanBinary(app.getPublicId(), getBundle(), "app.zip",
        new Stage(Stage.ID_BUILD), false, null, null, null);
    // Wait for the policy evaluation to finish, so we don't leak persisted entities from this test.
    ScanTicket statusTicket = originalTicket;
    while (statusTicket.currentStep != statusTicket.totalSteps) {
      statusTicket = scanService.getTicket(app.getPublicId(), originalTicket.ticketId);
    }
  }

  @Test
  public void testGetTicket_Anon() {
    assertThatExceptionOfType(UnauthenticatedException.class)
        .isThrownBy(() -> scanService.getTicket("any-app-id", "any-ticket-id"));
  }

  @Test
  public void testGetTicket_Unauthorized() {
    login();
    assertThatExceptionOfType(UnauthorizedException.class)
        .isThrownBy(() -> scanService.getTicket(app.getPublicId(), "any-ticket-id"));
  }

  @Test
  public void testGetTicket_Authorized() {
    grantPermission(app.getId(), Permission.EVALUATE_APPLICATION);
    try {
      scanService.getTicket(app.getPublicId(), "any-ticket-id");
    }
    catch (NotFoundException irrelevant) {
      // Expected but irrelevant for this test.
    }
  }
}
