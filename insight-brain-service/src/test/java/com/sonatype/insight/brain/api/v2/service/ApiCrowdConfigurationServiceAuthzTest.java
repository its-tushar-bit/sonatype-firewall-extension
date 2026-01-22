/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiCrowdConfigurationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiCrowdConfigurationService service;

  @Test(expected = UnauthenticatedException.class)
  public void testGetCrowdConfiguration_Unauthenticated() {
    service.getCrowdConfiguration();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetCrowdConfiguration_Unauthorized() {
    login();
    service.getCrowdConfiguration();
  }

  @Test(expected = NotFoundException.class)
  public void testGetCrowdConfiguration_Authorized() {
    grantConfigureSystemPermission();
    service.getCrowdConfiguration();
  }
  
  @Test(expected = UnauthenticatedException.class)
  public void testInsertOrUpdateCrowdConfiguration_Unauthenticated() {
    service.insertOrUpdateCrowdConfiguration(null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testInsertOrUpdateCrowdConfiguration_Unauthorized() {
    login();
    service.insertOrUpdateCrowdConfiguration(null);
  }

  @Test(expected = BadRequestException.class)
  public void testInsertOrUpdateCrowdConfiguration_Authorized() {
    grantConfigureSystemPermission();
    service.insertOrUpdateCrowdConfiguration(null);
  }
  
  @Test(expected = UnauthenticatedException.class)
  public void testDeleteCrowdConfiguration_Unauthenticated() {
    service.deleteCrowdConfiguration();
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteCrowdConfiguration_Unauthorized() {
    login();
    service.deleteCrowdConfiguration();
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteCrowdConfiguration_Authorized() {
    grantConfigureSystemPermission();
    service.deleteCrowdConfiguration();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testTestCrowdConfiguration_Unauthenticated() {
    service.testCrowdConfiguration(null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testTestCrowdConfiguration_Unauthorized() {
    login();
    service.testCrowdConfiguration(null);
  }

  @Test(expected = NotFoundException.class)
  public void testTestCrowdConfiguration_Authorized() {
    grantConfigureSystemPermission();
    service.testCrowdConfiguration(null);
  }
}
