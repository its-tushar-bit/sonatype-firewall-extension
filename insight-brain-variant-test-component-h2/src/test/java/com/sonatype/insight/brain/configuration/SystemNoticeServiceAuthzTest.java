/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.configuration.SystemNotice;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class SystemNoticeServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private SystemNoticeService service;

  @Test
  public void testUpdateSystemNotice_whenAuthorizedForConfigureSystem() {
    grantConfigureSystemPermission();
    service.updateSystemNotice(new SystemNotice());
  }

  @Test
  public void testUpdateSystemNotice_failsWhenNotAuthenticated() {
    assertThrows(UnauthenticatedException.class, () -> service.updateSystemNotice(new SystemNotice()));
  }

  @Test
  public void testUpdateSystemNotice_failsWhenNotAuthorizedForConfigureSystem() {
    login();
    assertThrows(UnauthorizedException.class, () -> service.updateSystemNotice(new SystemNotice()));
  }
}
