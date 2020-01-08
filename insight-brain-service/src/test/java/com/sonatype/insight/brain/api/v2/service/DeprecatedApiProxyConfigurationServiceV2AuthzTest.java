/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.DeprecatedApiProxyConfigurationDTOV2;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

/**
 * @deprecated The tested class is deprecated.
 */
@Deprecated
public class DeprecatedApiProxyConfigurationServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private DeprecatedApiProxyConfigurationServiceV2 proxyConfigurationService;

  @Test
  public void testGet_Unauthenticated() {
    tempEntity.setProxyServerConfiguration("localhost", 80);
    proxyConfigurationService.get();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdate_Unauthenticated() {
    proxyConfigurationService.update(new DeprecatedApiProxyConfigurationDTOV2());
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdate_Unauthorized() {
    login();
    proxyConfigurationService.update(new DeprecatedApiProxyConfigurationDTOV2());
  }

  @Test
  public void testUpdate_Authorized() {
    tempEntity.setProxyServerConfiguration("localhost", 80);
    grantConfigureSystemPermission();
    proxyConfigurationService.update(new DeprecatedApiProxyConfigurationDTOV2());
  }
}
