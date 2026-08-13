/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class AutomaticSourceControlConfigurationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private AutomaticSourceControlConfigurationService service;

  @Test
  public void testUpdate_Authorized() {
    grantManageAutomaticSourceControlPermission();
    service.update(new AutomaticSourceControlConfiguration(true));
  }

  @Test
  public void testUpdate_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.update(new AutomaticSourceControlConfiguration(true)));
  }

  @Test
  public void testUpdate_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.update(new AutomaticSourceControlConfiguration(true)));
  }

  @Test
  public void testGet_Authorized() {
    grantManageAutomaticSourceControlPermission();
    service.get();
  }

  @Test
  public void testGet_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.get());
  }

  @Test
  public void testGet_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.get());
  }
}
