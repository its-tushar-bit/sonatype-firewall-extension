/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiHashComponentIdentifierDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiHashComponentIdentifierServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiHashComponentIdentifierService apiHashComponentIdentifierService;

  @Test(expected = UnauthenticatedException.class)
  public void testGet_Unauthenticated() {
    apiHashComponentIdentifierService.get("hash");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGet_Unauthorized() {
    login();
    apiHashComponentIdentifierService.get("hash");
  }

  @Test(expected = NotFoundException.class)
  public void testGet_Authorized() {
    grantClaimComponentPermission();
    apiHashComponentIdentifierService.get("hash");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAll_Unauthenticated() {
    apiHashComponentIdentifierService.getAll();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAll_Unauthorized() {
    login();
    apiHashComponentIdentifierService.getAll();
  }

  @Test
  public void testGetAll_Authorized() {
    grantClaimComponentPermission();
    apiHashComponentIdentifierService.getAll();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testSet_Unauthenticated() {
    apiHashComponentIdentifierService.set(new ApiHashComponentIdentifierDTO());
  }

  @Test(expected = UnauthorizedException.class)
  public void testSet_Unauthorized() {
    login();
    apiHashComponentIdentifierService.set(new ApiHashComponentIdentifierDTO());
  }

  @Test(expected = BadRequestException.class)
  public void testSet_Authorized() {
    grantClaimComponentPermission();
    apiHashComponentIdentifierService.set(new ApiHashComponentIdentifierDTO());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDelete_Unauthenticated() {
    apiHashComponentIdentifierService.delete("hash");
  }

  @Test(expected = UnauthorizedException.class)
  public void testDelete_Unauthorized() {
    login();
    apiHashComponentIdentifierService.delete("hash");
  }

  @Test(expected = NotFoundException.class)
  public void testDelete_Authorized() {
    grantClaimComponentPermission();
    apiHashComponentIdentifierService.delete("hash");
  }
}
