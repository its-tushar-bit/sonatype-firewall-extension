/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class ApiReverseProxyAuthenticationConfigurationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiReverseProxyAuthenticationConfigurationService service;

  @Test
  public void testGetConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.getConfiguration());
  }

  @Test
  public void testGetConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.getConfiguration());
  }

  @Test
  public void testGetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(NotFoundException.class, () -> service.getConfiguration());
  }

  @Test
  public void testSetConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.setConfiguration(null));
  }

  @Test
  public void testSetConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.setConfiguration(null));
  }

  @Test
  public void testSetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(BadRequestException.class, () -> service.setConfiguration(null));
  }

  @Test
  public void testDeleteConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.deleteConfiguration());
  }

  @Test
  public void testDeleteConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.deleteConfiguration());
  }

  @Test
  public void testDeleteConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(NotFoundException.class, () -> service.deleteConfiguration());
  }
}
