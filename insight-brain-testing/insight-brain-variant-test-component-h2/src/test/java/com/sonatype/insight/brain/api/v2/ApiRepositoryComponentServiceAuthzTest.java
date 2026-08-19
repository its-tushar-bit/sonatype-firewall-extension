/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ApiRepositoryComponentServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private ApiRepositoryComponentService apiRepositoryComponentService;

  @Test
  public void testDeleteComponents_Authorized() {
    grantConfigureSystemPermission();
    apiRepositoryComponentService.deleteComponents(repositoryManager.getInstanceId(), List.of());
  }

  @Test
  public void testDeleteComponents_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiRepositoryComponentService.deleteComponents(repositoryManager.getInstanceId(), List.of()));
  }

  @Test
  public void testDeleteComponents_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiRepositoryComponentService.deleteComponents(repositoryManager.getInstanceId(), List.of()));
  }

  @Test
  public void testDeleteRepositoryComponents_Authorized() {
    grantConfigureSystemPermission();
    apiRepositoryComponentService.deleteRepositoryComponents(repositoryManager.getInstanceId(), List.of());
  }

  @Test
  public void testDeleteRepositoryComponents_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> apiRepositoryComponentService.deleteRepositoryComponents(repositoryManager.getInstanceId(), List.of()));
  }

  @Test
  public void testDeleteRepositoryComponents_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> apiRepositoryComponentService.deleteRepositoryComponents(repositoryManager.getInstanceId(), List.of()));
  }
}
