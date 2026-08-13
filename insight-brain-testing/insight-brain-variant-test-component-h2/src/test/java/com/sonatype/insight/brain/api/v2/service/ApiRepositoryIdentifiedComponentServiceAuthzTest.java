/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class ApiRepositoryIdentifiedComponentServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiRepositoryIdentifiedComponentService apiRepositoryIdentifiedComponentService;

  @Test
  public void testDelete_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiRepositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, null, null));
  }

  @Test
  public void testDelete_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiRepositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, null, null));
  }

  @Test
  public void testDelete_Authorized() {
    grantConfigureSystemPermission();
    assertThrows(BadRequestException.class,
        () -> apiRepositoryIdentifiedComponentService.deleteRepositoryIdentifiedComponent(null, null, null));
  }

  @Test
  public void testDeleteAllRepositoryIdentifiedComponents_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiRepositoryIdentifiedComponentService.deleteAllRepositoryIdentifiedComponents());
  }

  @Test
  public void testClearCache_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiRepositoryIdentifiedComponentService.deleteAllRepositoryIdentifiedComponents());
  }

  @Test
  public void testClearCache_Authorized() {
    grantConfigureSystemPermission();
    apiRepositoryIdentifiedComponentService.deleteAllRepositoryIdentifiedComponents();
  }
}
