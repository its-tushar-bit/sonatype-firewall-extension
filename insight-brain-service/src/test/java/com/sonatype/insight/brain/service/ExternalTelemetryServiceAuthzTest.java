/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.HashMap;

import javax.inject.Inject;

import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExternalTelemetryServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ExternalTelemetryService externalTelemetryService;

  @Test(expected = UnauthenticatedException.class)
  public void testSendTelemetry_Unauthenticated() {
    externalTelemetryService.sendTelemetry("", new HashMap<>());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSendTelemetry_Unauthorized() {
    login();
    externalTelemetryService.sendTelemetry("", new HashMap<>());
  }

  @Test
  public void testSetStatus_Authorized() {
    grantConfigureSystemPermission();
    assertThatThrownBy(() -> {
      externalTelemetryService.sendTelemetry(null, new HashMap<>());
    }).isInstanceOf(BadRequestException.class).hasMessage("Telemetry purpose is required.");
  }
}
