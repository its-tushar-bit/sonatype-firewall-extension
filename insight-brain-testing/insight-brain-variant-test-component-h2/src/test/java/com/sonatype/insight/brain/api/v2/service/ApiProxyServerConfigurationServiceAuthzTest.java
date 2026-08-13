/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiProxyServerConfigurationDTO;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiProxyServerConfigurationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiProxyServerConfigurationService proxyServerConfigurationService;

  @Test
  public void testGetConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> proxyServerConfigurationService.getConfiguration());
  }

  @Test
  public void testGetConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> proxyServerConfigurationService.getConfiguration());
  }

  @Test
  public void testGetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(NotFoundException.class, () -> proxyServerConfigurationService.getConfiguration());
  }

  @Test
  public void testSetConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> proxyServerConfigurationService.setConfiguration(new ApiProxyServerConfigurationDTO()));
  }

  @Test
  public void testSetConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> proxyServerConfigurationService.setConfiguration(new ApiProxyServerConfigurationDTO()));
  }

  @Test
  public void testSetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(BadRequestException.class,
        () -> proxyServerConfigurationService.setConfiguration(new ApiProxyServerConfigurationDTO()));
  }

  @Test
  public void testDeleteConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> proxyServerConfigurationService.deleteConfiguration());
  }

  @Test
  public void testDeleteConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> proxyServerConfigurationService.deleteConfiguration());
  }

  @Test
  public void testDeleteConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(NotFoundException.class, () -> proxyServerConfigurationService.deleteConfiguration());
  }
}
