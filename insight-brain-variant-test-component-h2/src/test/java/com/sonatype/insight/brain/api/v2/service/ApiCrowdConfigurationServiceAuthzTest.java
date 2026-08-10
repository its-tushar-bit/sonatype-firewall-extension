/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiCrowdConfigurationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiCrowdConfigurationService service;

  @Test
  public void testGetCrowdConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.getCrowdConfiguration());
  }

  @Test
  public void testGetCrowdConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.getCrowdConfiguration());
  }

  @Test
  public void testGetCrowdConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(NotFoundException.class, () -> service.getCrowdConfiguration());
  }

  @Test
  public void testInsertOrUpdateCrowdConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.insertOrUpdateCrowdConfiguration(null));
  }

  @Test
  public void testInsertOrUpdateCrowdConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.insertOrUpdateCrowdConfiguration(null));
  }

  @Test
  public void testInsertOrUpdateCrowdConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(BadRequestException.class, () -> service.insertOrUpdateCrowdConfiguration(null));
  }

  @Test
  public void testDeleteCrowdConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.deleteCrowdConfiguration());
  }

  @Test
  public void testDeleteCrowdConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.deleteCrowdConfiguration());
  }

  @Test
  public void testDeleteCrowdConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(NotFoundException.class, () -> service.deleteCrowdConfiguration());
  }

  @Test
  public void testTestCrowdConfiguration_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.testCrowdConfiguration(null));
  }

  @Test
  public void testTestCrowdConfiguration_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.testCrowdConfiguration(null));
  }

  @Test
  public void testTestCrowdConfiguration_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(NotFoundException.class, () -> service.testCrowdConfiguration(null));
  }
}
