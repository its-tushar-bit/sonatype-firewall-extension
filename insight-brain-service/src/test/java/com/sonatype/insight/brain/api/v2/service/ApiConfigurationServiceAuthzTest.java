/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiConfigurationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiConfigurationService service;

  @Test(expected = UnauthenticatedException.class)
  public void testGetConfiguration_Unauthenticated() {
    service.getConfiguration(Collections.emptySet());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetConfiguration_Unauthorized() {
    login();
    service.getConfiguration(Collections.emptySet());
  }

  @Test(expected = BadRequestException.class)
  public void testGetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    service.getConfiguration(Collections.emptySet());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSetConfiguration_Unauthenticated() {
    service.setConfiguration(Collections.emptyMap());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSetConfiguration_Unauthorized() {
    login();
    service.setConfiguration(Collections.emptyMap());
  }

  @Test(expected = BadRequestException.class)
  public void testSetConfiguration_Authorized() {
    grantConfigureSystemPermission();
    service.setConfiguration(Collections.emptyMap());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteConfiguration_Unauthenticated() {
    service.deleteConfiguration(Collections.emptySet());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteConfiguration_Unauthorized() {
    login();
    service.deleteConfiguration(Collections.emptySet());
  }

  @Test(expected = BadRequestException.class)
  public void testDeleteConfiguration_Authorized() {
    grantConfigureSystemPermission();
    service.deleteConfiguration(Collections.emptySet());
  }
}
