/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiHashComponentIdentifierDTO;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiHashComponentIdentifierServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiHashComponentIdentifierService apiHashComponentIdentifierService;

  @Test
  public void testGet_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> apiHashComponentIdentifierService.get("hash"));
  }

  @Test
  public void testGet_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> apiHashComponentIdentifierService.get("hash"));
  }

  @Test
  public void testGet_Authorized() {
    grantClaimComponentPermission();
    assertThrows(NotFoundException.class, () -> apiHashComponentIdentifierService.get("hash"));
  }

  @Test
  public void testGetAll_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> apiHashComponentIdentifierService.getAll());
  }

  @Test
  public void testGetAll_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> apiHashComponentIdentifierService.getAll());
  }

  @Test
  public void testGetAll_Authorized() {
    grantClaimComponentPermission();
    apiHashComponentIdentifierService.getAll();
  }

  @Test
  public void testSet_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiHashComponentIdentifierService.set(new ApiHashComponentIdentifierDTO()));
  }

  @Test
  public void testSet_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiHashComponentIdentifierService.set(new ApiHashComponentIdentifierDTO()));
  }

  @Test
  public void testSet_Authorized() {
    grantClaimComponentPermission();
    assertThrows(BadRequestException.class,
        () -> apiHashComponentIdentifierService.set(new ApiHashComponentIdentifierDTO()));
  }

  @Test
  public void testDelete_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> apiHashComponentIdentifierService.delete("hash"));
  }

  @Test
  public void testDelete_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> apiHashComponentIdentifierService.delete("hash"));
  }

  @Test
  public void testDelete_Authorized() {
    grantClaimComponentPermission();
    assertThrows(NotFoundException.class, () -> apiHashComponentIdentifierService.delete("hash"));
  }
}
