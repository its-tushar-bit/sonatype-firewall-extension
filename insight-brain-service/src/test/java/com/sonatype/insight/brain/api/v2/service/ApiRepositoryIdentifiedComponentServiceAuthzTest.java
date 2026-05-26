/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiRepositoryIdentifiedComponentServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiRepositoryIdentifiedComponentService apiRepositoryIdentifiedComponentService;

  @Test(expected = UnauthenticatedException.class)
  public void testDelete_Unauthenticated() {
    apiRepositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDelete_Unauthorized() {
    login();
    apiRepositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, null, null);
  }

  @Test(expected = BadRequestException.class)
  public void testDelete_Authorized() {
    grantConfigureSystemPermission();
    apiRepositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, null, null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteAllRepositoryIdentifiedComponents_Unauthenticated() {
    apiRepositoryIdentifiedComponentService.deleteAllRepositoryIdentifiedComponents();
  }

  @Test(expected = UnauthorizedException.class)
  public void testClearCache_Unauthorized() {
    login();
    apiRepositoryIdentifiedComponentService.deleteAllRepositoryIdentifiedComponents();
  }

  @Test
  public void testClearCache_Authorized() {
    grantConfigureSystemPermission();
    apiRepositoryIdentifiedComponentService.deleteAllRepositoryIdentifiedComponents();
  }
}
