/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiProxyServerConfigurationDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiProxyServerConfigurationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiProxyServerConfigurationService proxyServerConfigurationService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetConfiguration_Unauthenticated() {
    proxyServerConfigurationService.getConfiguration();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetConfiguration_Unauthorized() {
    login();
    proxyServerConfigurationService.getConfiguration();
  }

  @Test(expected = NotFoundException.class)
  public void testGetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    proxyServerConfigurationService.getConfiguration();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetConfiguration_Unauthenticated() {
    proxyServerConfigurationService.setConfiguration(new ApiProxyServerConfigurationDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetConfiguration_Unauthorized() {
    login();
    proxyServerConfigurationService.setConfiguration(new ApiProxyServerConfigurationDTO());
  }

  @Test(expected = BadRequestException.class)
  public void testSetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    proxyServerConfigurationService.setConfiguration(new ApiProxyServerConfigurationDTO());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteConfiguration_Unauthenticated() {
    proxyServerConfigurationService.deleteConfiguration();
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteConfiguration_Unauthorized() {
    login();
    proxyServerConfigurationService.deleteConfiguration();
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteConfiguration_Authorized() {
    grantConfigureSystemPermission();
    proxyServerConfigurationService.deleteConfiguration();
  }
}
