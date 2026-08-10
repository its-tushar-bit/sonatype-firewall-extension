/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiMailConfigurationDTO;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiMailConfigurationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiMailConfigurationService mailConfigurationService;

  @Test
  public void testGetConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> mailConfigurationService.getConfiguration());
  }

  @Test
  public void testGetConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> mailConfigurationService.getConfiguration());
  }

  @Test
  public void testGetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(NotFoundException.class, () -> mailConfigurationService.getConfiguration());
  }

  @Test
  public void testSetConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> mailConfigurationService.setConfiguration(new ApiMailConfigurationDTO()));
  }

  @Test
  public void testSetConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> mailConfigurationService.setConfiguration(new ApiMailConfigurationDTO()));
  }

  @Test
  public void testSetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(BadRequestException.class,
        () -> mailConfigurationService.setConfiguration(new ApiMailConfigurationDTO()));
  }

  @Test
  public void testDeleteConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> mailConfigurationService.deleteConfiguration());
  }

  @Test
  public void testDeleteConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> mailConfigurationService.deleteConfiguration());
  }

  @Test
  public void testDeleteConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(NotFoundException.class, () -> mailConfigurationService.deleteConfiguration());
  }

  @Test
  public void testTestConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> mailConfigurationService.testConfiguration("user@example.com", new ApiMailConfigurationDTO()));
  }

  @Test
  public void testTestConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> mailConfigurationService.testConfiguration("user@example.com", new ApiMailConfigurationDTO()));
  }

  @Test
  public void testTestConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(BadRequestException.class,
        () -> mailConfigurationService.testConfiguration("user@example.com", new ApiMailConfigurationDTO()));
  }
}
