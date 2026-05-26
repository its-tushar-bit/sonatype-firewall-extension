/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

public class AutomaticSourceControlConfigurationServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private AutomaticSourceControlConfigurationService service;

  @Test
  public void testUpdate_Authorized() {
    grantManageAutomaticSourceControlPermission();
    service.update(new AutomaticSourceControlConfiguration(true));
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdate_Unauthorized() {
    login();
    service.update(new AutomaticSourceControlConfiguration(true));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdate_Unauthenticated() {
    service.update(new AutomaticSourceControlConfiguration(true));
  }

  @Test
  public void testGet_Authorized() {
    grantManageAutomaticSourceControlPermission();
    service.get();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGet_Unauthorized() {
    login();
    service.get();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGet_Unauthenticated() {
    service.get();
  }
}
