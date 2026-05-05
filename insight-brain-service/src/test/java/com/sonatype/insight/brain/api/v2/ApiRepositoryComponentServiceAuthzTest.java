/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class ApiRepositoryComponentServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiRepositoryComponentService apiRepositoryComponentService;

  @Test
  public void testDeleteComponents_Authorized() {
    grantConfigureSystemPermission();
    apiRepositoryComponentService.deleteComponents(repositoryManager.getInstanceId(), List.of());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteComponents_Unauthenticated() {
    apiRepositoryComponentService.deleteComponents(repositoryManager.getInstanceId(), List.of());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteComponents_Unauthorized() {
    login();
    apiRepositoryComponentService.deleteComponents(repositoryManager.getInstanceId(), List.of());
  }

  @Test
  public void testDeleteRepositoryComponents_Authorized() {
    grantConfigureSystemPermission();
    apiRepositoryComponentService.deleteRepositoryComponents(repositoryManager.getInstanceId(), List.of());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteRepositoryComponents_Unauthenticated() {
    apiRepositoryComponentService.deleteRepositoryComponents(repositoryManager.getInstanceId(), List.of());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteRepositoryComponents_Unauthorized() {
    login();
    apiRepositoryComponentService.deleteRepositoryComponents(repositoryManager.getInstanceId(), List.of());
  }
}
