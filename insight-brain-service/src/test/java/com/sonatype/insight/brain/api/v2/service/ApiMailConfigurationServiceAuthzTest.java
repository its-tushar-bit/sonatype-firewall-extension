/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiMailConfigurationDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiMailConfigurationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiMailConfigurationService mailConfigurationService;

  @Test(expected = UnauthenticatedException.class)
  public void testGetConfiguration_Unauthenticated() {
    mailConfigurationService.getConfiguration();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetConfiguration_Unauthorized() {
    login();
    mailConfigurationService.getConfiguration();
  }

  @Test(expected = NotFoundException.class)
  public void testGetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    mailConfigurationService.getConfiguration();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetConfiguration_Unauthenticated() {
    mailConfigurationService.setConfiguration(new ApiMailConfigurationDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetConfiguration_Unauthorized() {
    login();
    mailConfigurationService.setConfiguration(new ApiMailConfigurationDTO());
  }

  @Test(expected = BadRequestException.class)
  public void testSetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    mailConfigurationService.setConfiguration(new ApiMailConfigurationDTO());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteConfiguration_Unauthenticated() {
    mailConfigurationService.deleteConfiguration();
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteConfiguration_Unauthorized() {
    login();
    mailConfigurationService.deleteConfiguration();
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteConfiguration_Authorized() {
    grantConfigureSystemPermission();
    mailConfigurationService.deleteConfiguration();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testTestConfiguration_Unauthenticated() {
    mailConfigurationService.testConfiguration("user@example.com", new ApiMailConfigurationDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testTestConfiguration_Unauthorized() {
    login();
    mailConfigurationService.testConfiguration("user@example.com", new ApiMailConfigurationDTO());
  }

  @Test(expected = BadRequestException.class)
  public void testTestConfiguration_Authorized() {
    grantConfigureSystemPermission();
    mailConfigurationService.testConfiguration("user@example.com", new ApiMailConfigurationDTO());
  }
}
